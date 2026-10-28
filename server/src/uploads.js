// Photo uploads: POST /uploads (multer, field `file`) and static GET /uploads/*.
const fs = require('fs');
const path = require('path');
const express = require('express');
const multer = require('multer');
const { randomUUID } = require('crypto');
const { sendError } = require('./errors');

const MAX_BYTES = 15 * 1024 * 1024;

const TYPES = {
  'image/jpeg': { ext: '.jpg', matches: (b) => b[0] === 0xff && b[1] === 0xd8 && b[2] === 0xff },
  'image/png': {
    ext: '.png',
    matches: (b) => b.subarray(0, 8).equals(Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a])),
  },
  'image/webp': {
    ext: '.webp',
    matches: (b) => b.toString('latin1', 0, 4) === 'RIFF' && b.toString('latin1', 8, 12) === 'WEBP',
  },
};
const TYPE_ERROR = 'Only JPEG, PNG or WebP images are allowed';

function readHead(file, length = 12) {
  const fd = fs.openSync(file, 'r');
  try {
    const buf = Buffer.alloc(length);
    const n = fs.readSync(fd, buf, 0, length, 0);
    return buf.subarray(0, n);
  } finally {
    fs.closeSync(fd);
  }
}

function uploadErrorMessage(err) {
  if (err instanceof multer.MulterError) {
    if (err.code === 'LIMIT_FILE_SIZE') return 'File is larger than 15 MB';
    if (err.code === 'LIMIT_UNEXPECTED_FILE') return 'Send exactly one file in the field "file"';
    return err.message;
  }
  return err.message;
}

/**
 * POST /uploads (after auth): stores the file as uploads/<random>.<ext> and
 * answers 201 { url: "/uploads/<random>.<ext>" }. The URL is relative
 * (ADR-0001), so it works whichever host a client uses.
 */
function uploadPhoto(uploadsDir) {
  const upload = multer({
    storage: multer.diskStorage({
      destination: uploadsDir,
      filename: (req, file, cb) => cb(null, randomUUID() + TYPES[file.mimetype].ext),
    }),
    limits: { fileSize: MAX_BYTES, files: 1 },
    fileFilter: (req, file, cb) => (TYPES[file.mimetype] ? cb(null, true) : cb(new Error(TYPE_ERROR))),
  }).single('file');

  return (req, res) => {
    upload(req, res, (err) => {
      if (err) {
        const message = uploadErrorMessage(err);
        return sendError(res, 400, message, { file: message });
      }
      if (!req.file) return sendError(res, 400, 'Missing file', { file: 'Send the photo as multipart field "file"' });
      // The declared type must match the content, so a renamed PDF is refused too.
      if (!TYPES[req.file.mimetype].matches(readHead(req.file.path))) {
        fs.rmSync(req.file.path, { force: true });
        return sendError(res, 400, TYPE_ERROR, { file: TYPE_ERROR });
      }
      res.status(201).json({ url: `/uploads/${req.file.filename}` });
    });
  };
}

/** GET /uploads/* served from disk without auth; a missing file is 404, not 401. */
function servePhotos(uploadsDir) {
  const router = express.Router();
  router.use('/uploads', express.static(uploadsDir, { index: false, dotfiles: 'ignore' }));
  router.get('/uploads/*', (req, res) => sendError(res, 404, 'Not found'));
  return router;
}

function ensureUploadsDir(uploadsDir) {
  fs.mkdirSync(path.resolve(uploadsDir), { recursive: true });
}

module.exports = { uploadPhoto, servePhotos, ensureUploadsDir, MAX_BYTES };
