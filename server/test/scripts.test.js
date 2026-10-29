// Issue 01: `npm run dev` entry point and `npm run reset`.
const { test, describe } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('fs');
const path = require('path');
const { spawn } = require('child_process');
const { SERVER_ROOT, makeDataDir, freePort, startServer, runReset, readSeed, assertErrorBody } = require('./helpers');
const config = require('../src/config');
const pkg = require('../package.json');

describe('npm scripts', () => {
  test('dev runs server.js, reset runs the reset script, default port is 3001', () => {
    assert.equal(pkg.scripts.dev, 'node server.js');
    assert.equal(pkg.scripts.reset, 'node scripts/reset.js');
    assert.equal(config.DEFAULT_PORT, 3001);
    assert.match(pkg.dependencies['json-server'], /^0\.17\./);
  });

  test('server.js creates db.json from the seed when missing and serves', async () => {
    const { dir, dbFile, uploadsDir } = makeDataDir();
    fs.rmSync(dbFile);
    const port = await freePort();
    const child = spawn(process.execPath, ['server.js'], {
      cwd: SERVER_ROOT,
      env: { ...process.env, PORT: String(port), DB_FILE: dbFile, UPLOADS_DIR: uploadsDir },
      stdio: ['ignore', 'pipe', 'pipe'],
    });
    try {
      await new Promise((resolve, reject) => {
        let out = '';
        child.stdout.on('data', (d) => {
          out += d;
          if (out.includes(`localhost:${port}`)) resolve();
        });
        child.stderr.on('data', (d) => (out += d));
        child.on('exit', (code) => reject(new Error(`server exited ${code}: ${out}`)));
      });
      assert.deepEqual(JSON.parse(fs.readFileSync(dbFile, 'utf8')), readSeed());
      const unauth = await fetch(`http://127.0.0.1:${port}/categories`);
      assert.equal(unauth.status, 401);
      const login = await fetch(`http://127.0.0.1:${port}/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: 'ops@example.com', password: 'password' }),
      });
      assert.equal(login.status, 200);
    } finally {
      child.kill();
      fs.rmSync(dir, { recursive: true, force: true });
    }
  });
});

describe('npm run reset', () => {
  test('restores db.json from the seed and invalidates every token, even while running', async () => {
    const srv = await startServer();
    try {
      const tech = await srv.login('tech1@example.com');
      const ops = await srv.login('ops@example.com');
      assert.equal((await srv.request('GET', '/categories', { token: tech })).status, 200);
      assert.equal(srv.readDb().sessions.length, 2);

      runReset(srv);

      assert.deepEqual(srv.readDb(), readSeed());
      assertErrorBody(assert, await srv.request('GET', '/categories', { token: tech }), 401);
      assertErrorBody(assert, await srv.request('GET', '/categories', { token: ops }), 401);
      // The server keeps working on the reset data.
      const fresh = await srv.login('tech1@example.com');
      assert.equal((await srv.request('GET', '/categories', { token: fresh })).status, 200);
      assert.equal(srv.readDb().sessions.length, 1);
    } finally {
      await srv.close();
    }
  });

  test('keeps uploads/seed/', async () => {
    const { dir, dbFile, uploadsDir } = makeDataDir();
    try {
      runReset({ dbFile, uploadsDir });
      const seedPhotos = fs.readdirSync(path.join(SERVER_ROOT, 'uploads', 'seed')).sort();
      assert.deepEqual(fs.readdirSync(path.join(uploadsDir, 'seed')).sort(), seedPhotos);
    } finally {
      fs.rmSync(dir, { recursive: true, force: true });
    }
  });
});
