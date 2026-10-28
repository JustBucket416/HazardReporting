// Issue 01: seed, auth, catalog, error body, DELETE 405, role guard.
const { test, before, after, describe } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('fs');
const path = require('path');
const { startServer, readSeed, assertErrorBody, SEED_UPLOADS } = require('./helpers');
const { requireRole } = require('../src/auth');

let srv;
before(async () => {
  srv = await startServer();
});
after(() => srv.close());

describe('seed', () => {
  const seed = readSeed();

  test('has the three demo accounts with password "password"', () => {
    const byEmail = Object.fromEntries(seed.users.map((u) => [u.email, u]));
    assert.equal(byEmail['tech1@example.com'].role, 'technician');
    assert.equal(byEmail['tech2@example.com'].role, 'technician');
    assert.equal(byEmail['ops@example.com'].role, 'operator');
    for (const u of seed.users) assert.equal(u.password, 'password');
  });

  test('has the catalog from the spec', () => {
    assert.deepEqual(
      seed.categories.map((c) => c.name),
      ['Equipment', 'Electrical', 'Plumbing', 'HVAC', 'Structural', 'Road/Grounds Hazard', 'Other'],
    );
    assert.deepEqual(
      seed.severities.map((s) => [s.name, s.rank]),
      [['Low', 1], ['Medium', 2], ['High', 3], ['Critical', 4]],
    );
    assert.deepEqual(seed.sites.map((s) => s.name), ['North Plant', 'Depot A', 'HQ Campus']);
    for (const s of seed.sites) {
      assert.equal(typeof s.lat, 'number');
      assert.equal(typeof s.lng, 'number');
    }
  });

  test('has about 8 incidents across both technicians covering every status', () => {
    assert.ok(seed.incidents.length >= 7 && seed.incidents.length <= 10);
    const reporters = new Set(seed.incidents.map((i) => i.reporterId));
    assert.deepEqual([...reporters].sort(), ['u-tech1', 'u-tech2']);
    const statuses = new Set(seed.incidents.map((i) => i.status));
    for (const s of ['open', 'acknowledged', 'in_progress', 'resolved', 'rejected', 'withdrawn']) {
      assert.ok(statuses.has(s), `no seed incident with status ${s}`);
    }
    assert.deepEqual(seed.sessions, []);
  });

  test('incidents are well-formed and their photos exist under uploads/seed/', () => {
    const ids = (k) => new Set(seed[k].map((x) => x.id));
    const [cats, sevs, sites] = [ids('categories'), ids('severities'), ids('sites')];
    for (const inc of seed.incidents) {
      assert.equal(typeof inc.id, 'string');
      assert.ok(cats.has(inc.categoryId) && sevs.has(inc.severityId) && sites.has(inc.siteId), inc.id);
      for (const k of ['createdAt', 'receivedAt', 'updatedAt']) assert.ok(Number.isInteger(inc[k]), `${inc.id}.${k}`);
      assert.ok(inc.photoUrls.length >= 1 && inc.photoUrls.length <= 3, inc.id);
      for (const url of inc.photoUrls) {
        assert.match(url, /^\/uploads\/seed\/[^/]+$/);
        assert.ok(fs.existsSync(path.join(SEED_UPLOADS, path.basename(url))), url);
      }
      assert.ok(Array.isArray(inc.notes));
    }
  });
});

describe('POST /login', () => {
  test('tech1 gets a token and user, and a sessions row is created', async () => {
    const res = await srv.request('POST', '/login', { body: { email: 'tech1@example.com', password: 'password' } });
    assert.equal(res.status, 200);
    assert.equal(typeof res.body.token, 'string');
    assert.deepEqual(res.body.user, { id: 'u-tech1', email: 'tech1@example.com', name: 'Alex Turner', role: 'technician' });
    const session = srv.readDb().sessions.find((s) => s.id === res.body.token);
    assert.ok(session, 'session row missing');
    assert.equal(session.userId, 'u-tech1');
    assert.ok(Number.isInteger(session.createdAt));
  });

  test('each login creates a distinct token', async () => {
    const a = await srv.login('tech1@example.com');
    const b = await srv.login('tech1@example.com');
    assert.notEqual(a, b);
  });

  test('wrong password, unknown email and missing body are 401 with an error body', async () => {
    assertErrorBody(assert, await srv.request('POST', '/login', { body: { email: 'tech1@example.com', password: 'nope' } }), 401);
    assertErrorBody(assert, await srv.request('POST', '/login', { body: { email: 'who@example.com', password: 'password' } }), 401);
    assertErrorBody(assert, await srv.request('POST', '/login', { body: {} }), 401);
  });
});

describe('auth middleware and catalog', () => {
  for (const route of ['/categories', '/severities', '/sites']) {
    test(`GET ${route} is 401 without a token or with an unknown one`, async () => {
      assertErrorBody(assert, await srv.request('GET', route), 401);
      assertErrorBody(assert, await srv.request('GET', route, { token: 'not-a-token' }), 401);
    });
  }

  test('catalog routes return 200 and the seed data for any role', async () => {
    const seed = readSeed();
    for (const email of ['tech1@example.com', 'ops@example.com']) {
      const token = await srv.login(email);
      for (const name of ['categories', 'severities', 'sites']) {
        const res = await srv.request('GET', `/${name}`, { token });
        assert.equal(res.status, 200);
        assert.deepEqual(res.body, seed[name]);
      }
    }
  });
});

describe('POST /logout', () => {
  test('deletes the session; the old token then gets 401', async () => {
    const token = await srv.login('tech2@example.com');
    assert.equal((await srv.request('GET', '/categories', { token })).status, 200);
    const res = await srv.request('POST', '/logout', { token });
    assert.equal(res.status, 204);
    assert.equal(srv.readDb().sessions.find((s) => s.id === token), undefined);
    assertErrorBody(assert, await srv.request('GET', '/categories', { token }), 401);
  });

  test('without a token is 401', async () => {
    assertErrorBody(assert, await srv.request('POST', '/logout'), 401);
  });
});

describe('everyone', () => {
  test('DELETE returns 405 on every route, with or without a token', async () => {
    const token = await srv.login('ops@example.com');
    for (const route of ['/incidents/inc-0001', '/incidents', '/categories', '/sites/site-depot-a', '/login', '/uploads/seed/photo-01.png', '/nope']) {
      assertErrorBody(assert, await srv.request('DELETE', route), 405);
      assertErrorBody(assert, await srv.request('DELETE', route, { token }), 405);
    }
    assert.equal(srv.readDb().incidents.length, readSeed().incidents.length);
  });

  test('json-server internals and unknown routes are not exposed', async () => {
    const token = await srv.login('ops@example.com');
    for (const route of ['/db', '/sessions', '/nope', '/categories/cat-hvac']) {
      assertErrorBody(assert, await srv.request('GET', route, { token }), 404);
    }
    for (const [method, route] of [['POST', '/categories'], ['PUT', '/sites'], ['PATCH', '/severities']]) {
      assertErrorBody(assert, await srv.request(method, route, { token, body: { name: 'x' } }), 405);
    }
    assertErrorBody(assert, await srv.request('GET', '/login', { token }), 405);
    assert.deepEqual(srv.readDb().categories, readSeed().categories);
  });

  test('malformed JSON is a 400 with an error body', async () => {
    assertErrorBody(assert, await srv.request('POST', '/login', { body: '{"email": ' }), 400);
  });
});

describe('role guard', () => {
  function run(role) {
    let status = null;
    let body = null;
    let nextCalled = false;
    const res = { status(s) { status = s; return this; }, json(b) { body = b; } };
    requireRole('operator')({ user: role && { role } }, res, () => { nextCalled = true; });
    return { status, body, nextCalled };
  }

  test('passes the allowed role through', () => {
    assert.deepEqual(run('operator'), { status: null, body: null, nextCalled: true });
  });

  test('answers 403 with an error body for any other role', () => {
    const r = run('technician');
    assert.equal(r.status, 403);
    assert.equal(typeof r.body.error, 'string');
    assert.equal(r.nextCalled, false);
  });
});
