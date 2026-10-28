// Issue 04: photo uploads.
const { test, before, after, describe } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('fs');
const path = require('path');
const { startServer, runReset, assertErrorBody, SEED_UPLOADS } = require('./helpers');

const MB = 1024 * 1024;
const JPEG_HEAD = [0xff, 0xd8, 0xff, 0xe0];
const PNG_HEAD = [0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a];

let srv;
let tech;

before(async () => {
  srv = await startServer();
  tech = await srv.login('tech1@example.com');
});
after(() => srv.close());

function bytes(head, size) {
  const buf = Buffer.alloc(size, 0x5a);
  Buffer.from(head).copy(buf);
  return buf;
}
function webp(size) {
  const buf = Buffer.alloc(size, 0);
  buf.write('RIFF', 0, 'latin1');
  buf.writeUInt32LE(size - 8, 4);
  buf.write('WEBPVP8 ', 8, 'latin1');
  return buf;
}

function form(buf, type, filename, field = 'file') {
  const fd = new FormData();
  fd.append(field, new Blob([buf], { type }), filename);
  return fd;
}

const upload = (body, token = tech) => srv.request('POST', '/uploads', { token, body });
const storedUploads = () => fs.readdirSync(srv.uploadsDir).filter((f) => f !== 'seed');

describe('POST /uploads', () => {
  test('a 5 MB JPEG returns 201 /uploads/<x>.jpg, and that URL loads without a token', async () => {
    const buf = bytes(JPEG_HEAD, 5 * MB);
    const res = await upload(form(buf, 'image/jpeg', 'photo.jpg'));
    assert.equal(res.status, 201);
    assert.match(res.body.url, /^\/uploads\/[0-9a-f-]{36}\.jpg$/);
    assert.deepEqual(Object.keys(res.body), ['url']);

    const onDisk = path.join(srv.uploadsDir, path.basename(res.body.url));
    assert.equal(fs.statSync(onDisk).size, buf.length);

    const got = await fetch(srv.baseUrl + res.body.url);
    assert.equal(got.status, 200);
    assert.equal(got.headers.get('content-type'), 'image/jpeg');
    assert.ok(Buffer.from(await got.arrayBuffer()).equals(buf));
  });

  test('accepts PNG and WebP, and exactly 15 MB', async () => {
    const png = await upload(form(bytes(PNG_HEAD, 1000), 'image/png', 'a.png'));
    assert.equal(png.status, 201);
    assert.match(png.body.url, /^\/uploads\/[^/]+\.png$/);

    const wp = await upload(form(webp(1000), 'image/webp', 'a.webp'));
    assert.equal(wp.status, 201);
    assert.match(wp.body.url, /^\/uploads\/[^/]+\.webp$/);

    const max = await upload(form(bytes(JPEG_HEAD, 15 * MB), 'image/jpeg', 'max.jpg'));
    assert.equal(max.status, 201);
  });

  test('each upload gets a new random name', async () => {
    const a = await upload(form(bytes(JPEG_HEAD, 100), 'image/jpeg', 'same.jpg'));
    const b = await upload(form(bytes(JPEG_HEAD, 100), 'image/jpeg', 'same.jpg'));
    assert.notEqual(a.body.url, b.body.url);
    assert.ok(!a.body.url.includes('same'));
  });

  test('a PDF and a 16 MB image are both rejected with 400, and nothing is stored', async () => {
    const before = storedUploads();
    const pdf = await upload(form(Buffer.from('%PDF-1.7\n%...\n'), 'application/pdf', 'doc.pdf'));
    assertErrorBody(assert, pdf, 400);
    assert.ok(pdf.body.fields.file);

    const big = await upload(form(bytes(JPEG_HEAD, 16 * MB), 'image/jpeg', 'big.jpg'));
    assertErrorBody(assert, big, 400);
    assert.match(big.body.error, /15 MB/);
    assert.deepEqual(storedUploads(), before);
  });

  test('content that does not match the declared image type is rejected with 400', async () => {
    const before = storedUploads();
    const disguised = await upload(form(Buffer.from('%PDF-1.7\n'.repeat(10)), 'image/jpeg', 'doc.jpg'));
    assertErrorBody(assert, disguised, 400);
    assert.deepEqual(storedUploads(), before);
  });

  test('a request without a "file" field is rejected with 400', async () => {
    assertErrorBody(assert, await upload(form(bytes(JPEG_HEAD, 100), 'image/jpeg', 'a.jpg', 'photo')), 400);
    assertErrorBody(assert, await upload(new FormData()), 400);
    assertErrorBody(assert, await srv.request('POST', '/uploads', { token: tech, body: { url: 'x' } }), 400);
  });

  test('an upload without a token returns 401 and stores nothing', async () => {
    const before = storedUploads();
    assertErrorBody(assert, await upload(form(bytes(JPEG_HEAD, 100), 'image/jpeg', 'a.jpg'), null), 401);
    assertErrorBody(assert, await upload(form(bytes(JPEG_HEAD, 100), 'image/jpeg', 'a.jpg'), 'bogus'), 401);
    assert.deepEqual(storedUploads(), before);
  });

  test('operators can upload too (any authenticated role)', async () => {
    const ops = await srv.login('ops@example.com');
    assert.equal((await upload(form(bytes(JPEG_HEAD, 100), 'image/jpeg', 'a.jpg'), ops)).status, 201);
  });
});

describe('GET /uploads/*', () => {
  test('seed photos are served without a token', async () => {
    const name = fs.readdirSync(SEED_UPLOADS)[0];
    const res = await fetch(`${srv.baseUrl}/uploads/seed/${name}`);
    assert.equal(res.status, 200);
    assert.equal(res.headers.get('content-type'), 'image/png');
    assert.ok(Buffer.from(await res.arrayBuffer()).equals(fs.readFileSync(path.join(SEED_UPLOADS, name))));
  });

  test('a missing upload is 404 (not 401) with an error body', async () => {
    assertErrorBody(assert, await srv.request('GET', '/uploads/nope.jpg'), 404);
    assertErrorBody(assert, await srv.request('GET', '/uploads/seed/nope.png'), 404);
  });
});

describe('npm run reset and uploads', () => {
  test('deletes every upload except uploads/seed/', async () => {
    const res = await upload(form(bytes(JPEG_HEAD, 100), 'image/jpeg', 'a.jpg'));
    fs.mkdirSync(path.join(srv.uploadsDir, 'stray-dir'));
    assert.ok(storedUploads().length > 0);

    runReset(srv);

    assert.deepEqual(fs.readdirSync(srv.uploadsDir), ['seed']);
    assert.deepEqual(fs.readdirSync(path.join(srv.uploadsDir, 'seed')).sort(), fs.readdirSync(SEED_UPLOADS).sort());
    assertErrorBody(assert, await srv.request('GET', res.body.url), 404);
    assert.equal((await fetch(`${srv.baseUrl}/uploads/seed/photo-01.png`)).status, 200);
  });
});
