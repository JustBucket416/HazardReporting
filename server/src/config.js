// Paths and port. Environment variables exist so tests can point the
// server and the reset script at a temporary copy of the data.
const path = require('path');

const SERVER_ROOT = path.resolve(__dirname, '..');
const DEFAULT_PORT = 3001;

module.exports = {
  DEFAULT_PORT,
  port: Number(process.env.PORT) || DEFAULT_PORT,
  seedFile: path.join(SERVER_ROOT, 'seed.json'),
  dbFile: process.env.DB_FILE ? path.resolve(process.env.DB_FILE) : path.join(SERVER_ROOT, 'db.json'),
  uploadsDir: process.env.UPLOADS_DIR ? path.resolve(process.env.UPLOADS_DIR) : path.join(SERVER_ROOT, 'uploads'),
};
