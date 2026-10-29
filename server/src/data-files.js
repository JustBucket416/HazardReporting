// Lifecycle of the data on disk: creating db.json from the seed, resetting
// it, and keeping json-server's in-memory copy in step with the file.
const fs = require('fs');
const path = require('path');

const SEED_UPLOADS = 'seed';

/** Replaces `target` with a copy of `source` atomically, so a running server never reads half a file. */
function copyAtomically(source, target) {
  const tmp = `${target}.${process.pid}.tmp`;
  fs.copyFileSync(source, tmp);
  fs.renameSync(tmp, target);
}

/** Creates db.json from the seed when it doesn't exist yet. */
function ensureDbFile(dbFile, seedFile) {
  if (!fs.existsSync(dbFile)) {
    fs.mkdirSync(path.dirname(dbFile), { recursive: true });
    copyAtomically(seedFile, dbFile);
  }
}

/**
 * Copies the seed over db.json (which also wipes `sessions`, invalidating every
 * token) and deletes everything in the uploads directory except `seed/`.
 */
function resetData({ seedFile, dbFile, uploadsDir }) {
  fs.mkdirSync(path.dirname(dbFile), { recursive: true });
  copyAtomically(seedFile, dbFile);

  fs.mkdirSync(uploadsDir, { recursive: true });
  const removed = [];
  for (const entry of fs.readdirSync(uploadsDir)) {
    if (entry === SEED_UPLOADS) continue;
    fs.rmSync(path.join(uploadsDir, entry), { recursive: true, force: true });
    removed.push(entry);
  }
  return { removed };
}

/**
 * Middleware that re-reads db.json before each request, so changes made on
 * disk while the server runs (`npm run reset`) take effect immediately
 * instead of being overwritten by the next write from memory.
 */
function reloadDbFromDisk(db, dbFile) {
  return (req, res, next) => {
    if (fs.existsSync(dbFile)) {
      try {
        db.read();
      } catch (err) {
        console.warn(`Could not re-read ${dbFile}; keeping the in-memory data. ${err.message}`);
      }
    }
    next();
  };
}

module.exports = { ensureDbFile, resetData, reloadDbFromDisk, SEED_UPLOADS };
