// Test harness: runs the app in-process on a free port against a temp copy of the seed.
const fs = require('fs');
const os = require('os');
const path = require('path');
const net = require('net');
const { spawnSync } = require('child_process');
const { createApp } = require('../server');

const SERVER_ROOT = path.resolve(__dirname, '..');
const SEED_FILE = path.join(SERVER_ROOT, 'seed.json');
const SEED_UPLOADS = path.join(SERVER_ROOT, 'uploads', 'seed');
const RESET_SCRIPT = path.join(SERVER_ROOT, 'scripts', 'reset.js');

function readSeed() {
  return JSON.parse(fs.readFileSync(SEED_FILE, 'utf8'));
}

/** A temp directory holding db.json (copied from the seed) and uploads/seed/. */
function makeDataDir() {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'hazard-server-'));
  const dbFile = path.join(dir, 'db.json');
  const uploadsDir = path.join(dir, 'uploads');
  fs.copyFileSync(SEED_FILE, dbFile);
  fs.cpSync(SEED_UPLOADS, path.join(uploadsDir, 'seed'), { recursive: true });
  return { dir, dbFile, uploadsDir };
}

async function freePort() {
  const srv = net.createServer();
  await new Promise((resolve) => srv.listen(0, '127.0.0.1', resolve));
  const { port } = srv.address();
  await new Promise((resolve) => srv.close(resolve));
  return port;
}

async function startServer() {
  const { dir, dbFile, uploadsDir } = makeDataDir();
  const app = createApp({ dbFile, uploadsDir, logger: false });
  const server = await new Promise((resolve) => {
    const s = app.listen(0, '127.0.0.1', () => resolve(s));
  });
  const baseUrl = `http://127.0.0.1:${server.address().port}`;

  /** fetch wrapper: JSON in, parsed JSON (or text) out. */
  async function request(method, urlPath, { token, body, headers = {} } = {}) {
    const init = { method, headers: { ...headers } };
    if (token) init.headers.Authorization = `Bearer ${token}`;
    if (body instanceof FormData) {
      init.body = body;
    } else if (typeof body === 'string') {
      init.headers['Content-Type'] = init.headers['Content-Type'] || 'application/json';
      init.body = body;
    } else if (body !== undefined) {
      init.headers['Content-Type'] = 'application/json';
      init.body = JSON.stringify(body);
    }
    const res = await fetch(baseUrl + urlPath, init);
    const text = await res.text();
    let parsed = null;
    if (text) {
      try {
        parsed = JSON.parse(text);
      } catch {
        parsed = text;
      }
    }
    return { status: res.status, headers: res.headers, body: parsed };
  }

  async function login(email, password = 'password') {
    const res = await request('POST', '/login', { body: { email, password } });
    if (res.status !== 200) throw new Error(`login ${email} failed: ${res.status}`);
    return res.body.token;
  }

  function readDb() {
    return JSON.parse(fs.readFileSync(dbFile, 'utf8'));
  }

  async function close() {
    server.closeAllConnections();
    await new Promise((resolve) => server.close(resolve));
    fs.rmSync(dir, { recursive: true, force: true });
  }

  return { baseUrl, dir, dbFile, uploadsDir, request, login, readDb, close };
}

/** Runs `scripts/reset.js` (what `npm run reset` runs) against the given data files. */
function runReset({ dbFile, uploadsDir }) {
  const result = spawnSync(process.execPath, [RESET_SCRIPT], {
    cwd: SERVER_ROOT,
    env: { ...process.env, DB_FILE: dbFile, UPLOADS_DIR: uploadsDir },
    encoding: 'utf8',
  });
  if (result.status !== 0) throw new Error(`reset failed: ${result.stderr}`);
  return result;
}

/** Asserts the shared 4xx body shape `{ error: string, fields?: object }`. */
function assertErrorBody(assert, res, status) {
  assert.equal(res.status, status, `expected ${status}, got ${res.status}: ${JSON.stringify(res.body)}`);
  assert.equal(typeof res.body?.error, 'string', `error body missing: ${JSON.stringify(res.body)}`);
  if ('fields' in res.body) assert.equal(typeof res.body.fields, 'object');
}

module.exports = {
  SERVER_ROOT,
  SEED_FILE,
  SEED_UPLOADS,
  readSeed,
  makeDataDir,
  freePort,
  startServer,
  runReset,
  assertErrorBody,
};
