// Issue 02: technician incident routes.
const { test, before, after, describe } = require('node:test');
const assert = require('node:assert/strict');
const { startServer, readSeed, assertErrorBody } = require('./helpers');

let srv;
let tech1;
let tech2;
let ops;
let nextCreatedAt = 1800000000000;

before(async () => {
  srv = await startServer();
  tech1 = await srv.login('tech1@example.com');
  tech2 = await srv.login('tech2@example.com');
  ops = await srv.login('ops@example.com');
});
after(() => srv.close());

function report(overrides = {}) {
  return {
    title: 'Broken light',
    description: 'Tube flickers in corridor 3',
    categoryId: 'cat-electrical',
    severityId: 'sev-low',
    siteId: 'site-north-plant',
    locationDescription: 'Corridor 3',
    position: { lat: 53.5, lng: -2.2, accuracyM: 12.5 },
    photoUrls: ['/uploads/a.jpg', '/uploads/b.jpg'],
    createdAt: nextCreatedAt++,
    ...overrides,
  };
}

function assertTechnicianView(incident) {
  assert.ok(!('assignee' in incident), 'technician view contains assignee');
  assert.ok(!('notes' in incident), 'technician view contains notes');
}

const dbIncident = (id) => srv.readDb().incidents.find((i) => i.id === id);

describe('GET /incidents/summary', () => {
  test("lists only the caller's incidents, without withdrawn ones, newest createdAt first", async () => {
    const seed = readSeed().incidents;
    for (const [token, userId] of [[tech1, 'u-tech1'], [tech2, 'u-tech2']]) {
      const res = await srv.request('GET', '/incidents/summary', { token });
      assert.equal(res.status, 200);
      const expected = seed
        .filter((i) => i.reporterId === userId && i.status !== 'withdrawn')
        .sort((a, b) => b.createdAt - a.createdAt)
        .map((i) => i.id);
      assert.deepEqual(res.body.map((i) => i.id), expected);
      assert.ok(!res.body.some((i) => i.status === 'withdrawn'));
    }
    // tech1 has a withdrawn seed incident, which must be hidden.
    assert.ok(seed.some((i) => i.reporterId === 'u-tech1' && i.status === 'withdrawn'));
  });

  test('items carry exactly the summary fields', async () => {
    const res = await srv.request('GET', '/incidents/summary', { token: tech1 });
    for (const item of res.body) {
      assert.deepEqual(Object.keys(item).sort(), ['categoryId', 'createdAt', 'id', 'severityId', 'status', 'title', 'updatedAt']);
    }
  });

  test('is 401 without a token and 403 for an operator', async () => {
    assertErrorBody(assert, await srv.request('GET', '/incidents/summary'), 401);
    assertErrorBody(assert, await srv.request('GET', '/incidents/summary', { token: ops }), 403);
  });
});

describe('GET /incidents/:id (technician)', () => {
  test('returns the full own incident without assignee and notes', async () => {
    const res = await srv.request('GET', '/incidents/inc-0003', { token: tech1 });
    assert.equal(res.status, 200);
    assertTechnicianView(res.body);
    const { assignee, notes, ...expected } = readSeed().incidents.find((i) => i.id === 'inc-0003');
    assert.deepEqual(res.body, expected);
  });

  test('own withdrawn incidents are still readable by id', async () => {
    const res = await srv.request('GET', '/incidents/inc-0006', { token: tech1 });
    assert.equal(res.status, 200);
    assert.equal(res.body.status, 'withdrawn');
  });

  test("is 404 for another technician's incident and for a missing one", async () => {
    assertErrorBody(assert, await srv.request('GET', '/incidents/inc-0007', { token: tech1 }), 404);
    assertErrorBody(assert, await srv.request('GET', '/incidents/inc-0008', { token: tech2 }), 404);
    assertErrorBody(assert, await srv.request('GET', '/incidents/does-not-exist', { token: tech1 }), 404);
  });
});

describe('POST /incidents', () => {
  test('creates an open incident and returns 200 with the technician view', async () => {
    const body = report();
    const before = Date.now();
    const res = await srv.request('POST', '/incidents', { token: tech1, body });
    assert.equal(res.status, 200);
    assertTechnicianView(res.body);
    assert.equal(typeof res.body.id, 'string');
    const { id, receivedAt, updatedAt, ...rest } = res.body;
    assert.deepEqual(rest, { ...body, reporterId: 'u-tech1', status: 'open', priority: null });
    assert.ok(receivedAt >= before && Number.isInteger(receivedAt));
    assert.equal(updatedAt, receivedAt);

    const stored = dbIncident(id);
    assert.equal(stored.assignee, null);
    assert.deepEqual(stored.notes, []);
    assert.equal(stored.reporterId, 'u-tech1');
  });

  test('the reporter comes from the token, never from the body', async () => {
    const res = await srv.request('POST', '/incidents', { token: tech1, body: { ...report(), reporterId: 'u-tech2' } });
    assertErrorBody(assert, res, 400);
    assert.equal(res.body.fields.reporterId, 'Not allowed');
  });

  test('re-POSTing the same createdAt leaves one incident, updated to the latest body, same id, both 200', async () => {
    const first = report({ title: 'Leak under sink' });
    const r1 = await srv.request('POST', '/incidents', { token: tech1, body: first });
    const second = { ...first, title: 'Leak under sink (worse)', severityId: 'sev-high', photoUrls: ['/uploads/c.jpg'] };
    const r2 = await srv.request('POST', '/incidents', { token: tech1, body: second });
    assert.equal(r1.status, 200);
    assert.equal(r2.status, 200);
    assert.equal(r2.body.id, r1.body.id);
    assertTechnicianView(r2.body);

    const matches = srv.readDb().incidents.filter((i) => i.reporterId === 'u-tech1' && i.createdAt === first.createdAt);
    assert.equal(matches.length, 1);
    assert.equal(matches[0].title, 'Leak under sink (worse)');
    assert.equal(matches[0].severityId, 'sev-high');
    assert.deepEqual(matches[0].photoUrls, ['/uploads/c.jpg']);
    assert.equal(matches[0].receivedAt, r1.body.receivedAt);
    assert.ok(matches[0].updatedAt >= r1.body.updatedAt);
  });

  test('two POSTs with the same title but different createdAt create two incidents', async () => {
    const r1 = await srv.request('POST', '/incidents', { token: tech1, body: report({ title: 'Same title' }) });
    const r2 = await srv.request('POST', '/incidents', { token: tech1, body: report({ title: 'Same title' }) });
    assert.equal(r1.status, 200);
    assert.equal(r2.status, 200);
    assert.notEqual(r1.body.id, r2.body.id);
    assert.equal(srv.readDb().incidents.filter((i) => i.title === 'Same title').length, 2);
  });

  test('the same createdAt from two different technicians creates two incidents', async () => {
    const createdAt = nextCreatedAt++;
    const r1 = await srv.request('POST', '/incidents', { token: tech1, body: report({ createdAt }) });
    const r2 = await srv.request('POST', '/incidents', { token: tech2, body: report({ createdAt }) });
    assert.notEqual(r1.body.id, r2.body.id);
    assert.equal(srv.readDb().incidents.filter((i) => i.createdAt === createdAt).length, 2);
  });

  test('a retry for an incident that is no longer open returns it unchanged (server wins)', async () => {
    const seeded = readSeed().incidents.find((i) => i.id === 'inc-0005'); // tech1, acknowledged
    const res = await srv.request('POST', '/incidents', {
      token: tech1,
      body: report({ createdAt: seeded.createdAt, title: 'Changed offline' }),
    });
    assert.equal(res.status, 200);
    assert.equal(res.body.id, 'inc-0005');
    assert.equal(res.body.status, 'acknowledged');
    assert.equal(res.body.title, seeded.title);
    assertTechnicianView(res.body);
    assert.equal(dbIncident('inc-0005').title, seeded.title);
  });

  test('optional fields default: description and locationDescription to "", position to null', async () => {
    const { description, locationDescription, position, ...body } = report();
    const res = await srv.request('POST', '/incidents', { token: tech1, body });
    assert.equal(res.status, 200);
    assert.equal(res.body.description, '');
    assert.equal(res.body.locationDescription, '');
    assert.equal(res.body.position, null);
  });

  test('accepts the boundaries: 80-char title, 1000-char description, 3 photos, 1 photo', async () => {
    for (const overrides of [
      { title: 'x'.repeat(80), description: 'd'.repeat(1000), photoUrls: ['/uploads/1.jpg', '/uploads/2.jpg', '/uploads/3.jpg'] },
      { photoUrls: ['/uploads/1.jpg'], position: null },
    ]) {
      assert.equal((await srv.request('POST', '/incidents', { token: tech1, body: report(overrides) })).status, 200);
    }
  });

  test('validation failures are 400 with a fields object and create nothing', async () => {
    const count = srv.readDb().incidents.length;
    const cases = [
      [{ title: '' }, 'title'],
      [{ title: '   ' }, 'title'],
      [{ title: 'x'.repeat(81) }, 'title'],
      [{ title: undefined }, 'title'],
      [{ description: 'd'.repeat(1001) }, 'description'],
      [{ categoryId: 'cat-nope' }, 'categoryId'],
      [{ severityId: 'sev-nope' }, 'severityId'],
      [{ siteId: undefined }, 'siteId'],
      [{ photoUrls: [] }, 'photoUrls'],
      [{ photoUrls: ['/a', '/b', '/c', '/d'] }, 'photoUrls'],
      [{ photoUrls: undefined }, 'photoUrls'],
      [{ position: { lat: 'x', lng: 1, accuracyM: 1 } }, 'position'],
      [{ createdAt: '1800000000000' }, 'createdAt'],
      [{ createdAt: 1.5 }, 'createdAt'],
      [{ createdAt: undefined }, 'createdAt'],
      [{ status: 'resolved' }, 'status'],
      [{ priority: 'high' }, 'priority'],
      [{ assignee: 'Crew' }, 'assignee'],
    ];
    for (const [overrides, field] of cases) {
      const res = await srv.request('POST', '/incidents', { token: tech1, body: report(overrides) });
      assertErrorBody(assert, res, 400);
      assert.ok(res.body.fields && field in res.body.fields, `${JSON.stringify(overrides)} -> ${JSON.stringify(res.body)}`);
    }
    assertErrorBody(assert, await srv.request('POST', '/incidents', { token: tech1, body: [report()] }), 400);
    assert.equal(srv.readDb().incidents.length, count);
  });

  test('is 401 without a token and 403 for an operator', async () => {
    assertErrorBody(assert, await srv.request('POST', '/incidents', { body: report() }), 401);
    assertErrorBody(assert, await srv.request('POST', '/incidents', { token: ops, body: report() }), 403);
  });
});

describe('PATCH /incidents/:id (technician)', () => {
  async function createOpen(token = tech1) {
    return (await srv.request('POST', '/incidents', { token, body: report() })).body;
  }

  test('updates a subset of report fields on an open incident', async () => {
    const created = await createOpen();
    const res = await srv.request('PATCH', `/incidents/${created.id}`, {
      token: tech1,
      body: { title: 'Edited', severityId: 'sev-critical', position: null },
    });
    assert.equal(res.status, 200);
    assertTechnicianView(res.body);
    assert.equal(res.body.title, 'Edited');
    assert.equal(res.body.severityId, 'sev-critical');
    assert.equal(res.body.position, null);
    assert.equal(res.body.description, created.description);
    assert.equal(res.body.status, 'open');
    assert.equal(res.body.createdAt, created.createdAt);
    assert.ok(res.body.updatedAt >= created.updatedAt);
    assert.equal(dbIncident(created.id).title, 'Edited');
  });

  test('withdraws an open incident with exactly { status: "withdrawn" }', async () => {
    const created = await createOpen();
    const res = await srv.request('PATCH', `/incidents/${created.id}`, { token: tech1, body: { status: 'withdrawn' } });
    assert.equal(res.status, 200);
    assert.equal(res.body.status, 'withdrawn');
    assertTechnicianView(res.body);
    assert.equal(dbIncident(created.id).status, 'withdrawn');

    const summary = await srv.request('GET', '/incidents/summary', { token: tech1 });
    assert.ok(!summary.body.some((i) => i.id === created.id));
    // Withdrawn is final for the technician too.
    assertErrorBody(assert, await srv.request('PATCH', `/incidents/${created.id}`, { token: tech1, body: { title: 'x' } }), 409);
  });

  test('is 409 when the incident is not open', async () => {
    for (const id of ['inc-0005' /* acknowledged */, 'inc-0003' /* in_progress */, 'inc-0001' /* resolved */, 'inc-0006' /* withdrawn */]) {
      const before = dbIncident(id);
      assertErrorBody(assert, await srv.request('PATCH', `/incidents/${id}`, { token: tech1, body: { title: 'Too late' } }), 409);
      assertErrorBody(assert, await srv.request('PATCH', `/incidents/${id}`, { token: tech1, body: { status: 'withdrawn' } }), 409);
      assert.deepEqual(dbIncident(id), before);
    }
  });

  test('is 400 when any other field is present', async () => {
    const created = await createOpen();
    const cases = [
      [{ priority: 'high' }, 'priority'],
      [{ title: 'ok', priority: 'high' }, 'priority'],
      [{ createdAt: 123 }, 'createdAt'],
      [{ assignee: 'Crew' }, 'assignee'],
      [{ notes: [] }, 'notes'],
      [{ reporterId: 'u-tech2' }, 'reporterId'],
      [{ status: 'resolved' }, 'status'],
      [{ status: 'open' }, 'status'],
      [{ status: 'withdrawn', title: 'x' }, 'title'],
      [{}, 'body'],
    ];
    for (const [body, field] of cases) {
      const res = await srv.request('PATCH', `/incidents/${created.id}`, { token: tech1, body });
      assertErrorBody(assert, res, 400);
      assert.ok(field in res.body.fields, `${JSON.stringify(body)} -> ${JSON.stringify(res.body)}`);
    }
    assert.equal(dbIncident(created.id).title, created.title);
    assert.equal(dbIncident(created.id).status, 'open');
    assert.equal(dbIncident(created.id).priority, null);
  });

  test('validates the report fields that are present', async () => {
    const created = await createOpen();
    for (const body of [{ title: '' }, { description: 'd'.repeat(1001) }, { siteId: 'nope' }, { photoUrls: [] }, { photoUrls: ['/1', '/2', '/3', '/4'] }]) {
      assertErrorBody(assert, await srv.request('PATCH', `/incidents/${created.id}`, { token: tech1, body }), 400);
    }
    assert.equal(dbIncident(created.id).title, created.title);
  });

  test("is 404 on another technician's incident, which stays unchanged", async () => {
    const theirs = await createOpen(tech2);
    assertErrorBody(assert, await srv.request('PATCH', `/incidents/${theirs.id}`, { token: tech1, body: { title: 'Mine now' } }), 404);
    assertErrorBody(assert, await srv.request('PATCH', `/incidents/${theirs.id}`, { token: tech1, body: { status: 'withdrawn' } }), 404);
    assertErrorBody(assert, await srv.request('PATCH', '/incidents/inc-0007', { token: tech1, body: { title: 'x' } }), 404);
    assert.equal(dbIncident(theirs.id).title, theirs.title);
    assert.equal(dbIncident(theirs.id).status, 'open');
  });

  test('is 401 without a token', async () => {
    assertErrorBody(assert, await srv.request('PATCH', '/incidents/inc-0008', { body: { title: 'x' } }), 401);
  });
});

describe('technician view', () => {
  test('never contains assignee or notes, on any technician route', async () => {
    const summary = await srv.request('GET', '/incidents/summary', { token: tech1 });
    summary.body.forEach(assertTechnicianView);
    for (const item of summary.body) {
      assertTechnicianView((await srv.request('GET', `/incidents/${item.id}`, { token: tech1 })).body);
    }
  });
});
