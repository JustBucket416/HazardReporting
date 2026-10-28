// Issue 03: operator routes and status transitions.
const { test, before, after, describe } = require('node:test');
const assert = require('node:assert/strict');
const { startServer, readSeed, assertErrorBody } = require('./helpers');

let srv;
let ops;
let tech1;
let nextCreatedAt = 1810000000000;

before(async () => {
  srv = await startServer();
  ops = await srv.login('ops@example.com');
  tech1 = await srv.login('tech1@example.com');
});
after(() => srv.close());

async function reportOpen(overrides = {}) {
  const res = await srv.request('POST', '/incidents', {
    token: tech1,
    body: {
      title: 'Operator test incident',
      description: '',
      categoryId: 'cat-other',
      severityId: 'sev-medium',
      siteId: 'site-hq-campus',
      locationDescription: '',
      position: null,
      photoUrls: ['/uploads/x.jpg'],
      createdAt: nextCreatedAt++,
      ...overrides,
    },
  });
  assert.equal(res.status, 200);
  return res.body;
}

const patch = (id, body, token = ops) => srv.request('PATCH', `/incidents/${id}`, { token, body });
const dbIncident = (id) => srv.readDb().incidents.find((i) => i.id === id);

describe('role checks', () => {
  test('technicians get 403 on operator-only routes; no token is 401', async () => {
    assertErrorBody(assert, await srv.request('GET', '/incidents', { token: tech1 }), 403);
    assertErrorBody(assert, await srv.request('GET', '/users', { token: tech1 }), 403);
    assertErrorBody(assert, await srv.request('GET', '/incidents'), 401);
    assertErrorBody(assert, await srv.request('GET', '/users'), 401);
  });

  test('operators get 403 on technician-only routes', async () => {
    assertErrorBody(assert, await srv.request('GET', '/incidents/summary', { token: ops }), 403);
  });
});

describe('GET /incidents', () => {
  test('returns every incident in full', async () => {
    const res = await srv.request('GET', '/incidents', { token: ops });
    assert.equal(res.status, 200);
    const seedById = Object.fromEntries(readSeed().incidents.map((i) => [i.id, i]));
    for (const [id, inc] of Object.entries(seedById)) {
      assert.deepEqual(res.body.find((i) => i.id === id), inc);
    }
  });

  test('?status=open&severityId=<critical>&_page=1&_limit=10 filters and sets X-Total-Count', async () => {
    for (let i = 0; i < 11; i++) await reportOpen({ severityId: 'sev-critical', title: `Critical ${i}` });
    await reportOpen({ severityId: 'sev-low' });

    const expected = srv.readDb().incidents.filter((i) => i.status === 'open' && i.severityId === 'sev-critical');
    assert.equal(expected.length, 12); // 11 new + seed inc-0007

    const page1 = await srv.request('GET', '/incidents?status=open&severityId=sev-critical&_page=1&_limit=10', { token: ops });
    assert.equal(page1.status, 200);
    assert.equal(page1.headers.get('x-total-count'), '12');
    assert.equal(page1.body.length, 10);
    for (const i of page1.body) {
      assert.equal(i.status, 'open');
      assert.equal(i.severityId, 'sev-critical');
    }
    const page2 = await srv.request('GET', '/incidents?status=open&severityId=sev-critical&_page=2&_limit=10', { token: ops });
    assert.equal(page2.headers.get('x-total-count'), '12');
    assert.equal(page2.body.length, 2);
    assert.deepEqual(
      [...page1.body, ...page2.body].map((i) => i.id).sort(),
      expected.map((i) => i.id).sort(),
    );
  });

  test('supports q, _sort/_order and createdAt_gte/createdAt_lte', async () => {
    const q = await srv.request('GET', '/incidents?q=pothole', { token: ops });
    assert.deepEqual(q.body.map((i) => i.id), ['inc-0007']);

    const sorted = await srv.request('GET', '/incidents?_sort=createdAt&_order=desc', { token: ops });
    const createdAts = sorted.body.map((i) => i.createdAt);
    assert.deepEqual(createdAts, [...createdAts].sort((a, b) => b - a));

    const seed = readSeed().incidents;
    const [from, to] = [seed[2].createdAt, seed[5].createdAt]; // inc-0003 .. inc-0006
    const range = await srv.request('GET', `/incidents?createdAt_gte=${from}&createdAt_lte=${to}&_sort=createdAt`, { token: ops });
    assert.deepEqual(range.body.map((i) => i.id), ['inc-0003', 'inc-0004', 'inc-0005', 'inc-0006']);

    const limited = await srv.request('GET', '/incidents?status=in_progress&_limit=1', { token: ops });
    assert.equal(limited.body.length, 1);
    assert.equal(limited.headers.get('x-total-count'), '2');
  });
});

describe('GET /incidents/:id (operator)', () => {
  test('returns the full incident, including assignee and notes', async () => {
    const res = await srv.request('GET', '/incidents/inc-0001', { token: ops });
    assert.equal(res.status, 200);
    assert.deepEqual(res.body, readSeed().incidents.find((i) => i.id === 'inc-0001'));
  });

  test('is 404 for a missing incident', async () => {
    assertErrorBody(assert, await srv.request('GET', '/incidents/nope', { token: ops }), 404);
  });
});

describe('PATCH /incidents/:id (operator)', () => {
  test('open -> acknowledged succeeds; open -> resolved is 409', async () => {
    const a = await reportOpen();
    const ok = await patch(a.id, { status: 'acknowledged' });
    assert.equal(ok.status, 200);
    assert.equal(ok.body.status, 'acknowledged');
    assert.ok('assignee' in ok.body && 'notes' in ok.body, 'operator gets the full incident');
    assert.ok(ok.body.updatedAt >= a.updatedAt);

    const b = await reportOpen();
    assertErrorBody(assert, await patch(b.id, { status: 'resolved' }), 409);
    assert.equal(dbIncident(b.id).status, 'open');
  });

  test('follows the whole transition table', async () => {
    const allowed = {
      open: ['acknowledged', 'rejected'],
      acknowledged: ['in_progress', 'rejected'],
      in_progress: ['resolved', 'rejected'],
    };
    const path = { open: [], acknowledged: ['acknowledged'], in_progress: ['acknowledged', 'in_progress'] };
    const all = ['open', 'acknowledged', 'in_progress', 'resolved', 'rejected', 'withdrawn'];
    for (const [from, targets] of Object.entries(allowed)) {
      for (const to of all) {
        if (to === from) continue;
        const inc = await reportOpen();
        for (const step of path[from]) assert.equal((await patch(inc.id, { status: step })).status, 200);
        const res = await patch(inc.id, { status: to });
        if (targets.includes(to)) {
          assert.equal(res.status, 200, `${from} -> ${to}`);
          assert.equal(dbIncident(inc.id).status, to);
        } else {
          assertErrorBody(assert, res, 409);
          assert.equal(dbIncident(inc.id).status, from, `${from} -> ${to} must be refused`);
        }
      }
    }
  });

  test('any change to a resolved, rejected or withdrawn incident is 409', async () => {
    // Seed: inc-0001 resolved, inc-0002 rejected, inc-0006 withdrawn.
    for (const id of ['inc-0001', 'inc-0002', 'inc-0006']) {
      const before = dbIncident(id);
      for (const body of [{ priority: 'high' }, { assignee: 'Crew' }, { note: 'late note' }, { status: 'in_progress' }, { status: before.status }]) {
        assertErrorBody(assert, await patch(id, body), 409);
      }
      assert.deepEqual(dbIncident(id), before);
    }
  });

  test('two added notes are both kept, in order, with server timestamps and the operator id', async () => {
    const seeded = dbIncident('inc-0003');
    const t0 = Date.now();
    const r1 = await patch('inc-0003', { note: 'First note' });
    const r2 = await patch('inc-0003', { note: '  Second note  ' });
    const t1 = Date.now();
    assert.equal(r1.status, 200);
    assert.equal(r2.status, 200);
    const notes = dbIncident('inc-0003').notes;
    assert.equal(notes.length, seeded.notes.length + 2);
    assert.deepEqual(notes.slice(0, seeded.notes.length), seeded.notes);
    const [n1, n2] = notes.slice(seeded.notes.length);
    assert.deepEqual([n1.text, n2.text], ['First note', 'Second note']);
    for (const n of [n1, n2]) {
      assert.equal(n.byUserId, 'u-ops');
      assert.ok(Number.isInteger(n.at) && n.at >= t0 && n.at <= t1);
      assert.deepEqual(Object.keys(n).sort(), ['at', 'byUserId', 'text']);
    }
    assert.ok(n2.at >= n1.at);
    assert.deepEqual(r2.body.notes, notes);
  });

  test('sets priority and assignee, together with a status change and a note', async () => {
    const inc = await reportOpen();
    const res = await patch(inc.id, { status: 'acknowledged', priority: 'urgent', assignee: '  Electrical team ', note: 'On it' });
    assert.equal(res.status, 200);
    const stored = dbIncident(inc.id);
    assert.equal(stored.status, 'acknowledged');
    assert.equal(stored.priority, 'urgent');
    assert.equal(stored.assignee, 'Electrical team');
    assert.equal(stored.notes.length, 1);

    assert.equal((await patch(inc.id, { priority: null, assignee: '' })).status, 200);
    assert.equal(dbIncident(inc.id).priority, null);
    assert.equal(dbIncident(inc.id).assignee, null);

    // Re-sending the current status is not a change and is accepted.
    assert.equal((await patch(inc.id, { status: 'acknowledged', priority: 'low' })).status, 200);
    assert.equal(dbIncident(inc.id).priority, 'low');
  });

  test('is 400 on any other field or an invalid value', async () => {
    const inc = await reportOpen();
    const cases = [
      [{ title: 'x' }, 'title'],
      [{ notes: [] }, 'notes'],
      [{ createdAt: 1 }, 'createdAt'],
      [{ reporterId: 'u-tech2' }, 'reporterId'],
      [{ status: 'closed' }, 'status'],
      [{ priority: 'critical' }, 'priority'],
      [{ assignee: 42 }, 'assignee'],
      [{ note: '' }, 'note'],
      [{ note: '   ' }, 'note'],
      [{ status: 'acknowledged', title: 'x' }, 'title'],
      [{}, 'body'],
    ];
    for (const [body, field] of cases) {
      const res = await patch(inc.id, body);
      assertErrorBody(assert, res, 400);
      assert.ok(field in res.body.fields, `${JSON.stringify(body)} -> ${JSON.stringify(res.body)}`);
    }
    const stored = dbIncident(inc.id);
    assert.equal(stored.status, 'open');
    assert.equal(stored.title, inc.title);
    assert.deepEqual(stored.notes, []);
  });

  test('is 404 for a missing incident', async () => {
    assertErrorBody(assert, await patch('nope', { priority: 'low' }), 404);
  });

  test("after triage the reporter gets 409 on edits and never sees the note or the assignee", async () => {
    const inc = await reportOpen();
    assert.equal((await patch(inc.id, { status: 'acknowledged', assignee: 'Crew A', note: 'internal' })).status, 200);
    assertErrorBody(assert, await patch(inc.id, { title: 'stale edit' }, tech1), 409);
    assertErrorBody(assert, await patch(inc.id, { status: 'withdrawn' }, tech1), 409);
    const seen = await srv.request('GET', `/incidents/${inc.id}`, { token: tech1 });
    assert.equal(seen.body.status, 'acknowledged');
    assert.ok(!('assignee' in seen.body) && !('notes' in seen.body));
  });
});

describe('GET /users', () => {
  test('lists users without password', async () => {
    const res = await srv.request('GET', '/users', { token: ops });
    assert.equal(res.status, 200);
    const expected = readSeed().users.map(({ password, ...u }) => u);
    assert.deepEqual(res.body, expected);
    assert.ok(!JSON.stringify(res.body).includes('password'));
  });
});
