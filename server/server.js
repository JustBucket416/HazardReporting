// Field Incident Reporter backend: json-server 0.17 used as a module, with our
// own middleware in front of its router enforcing auth, roles and the incident
// rules (ADR-0001). `node server.js` (npm run dev) listens on port 3001.
const path = require('path');
const jsonServer = require('json-server');
const config = require('./src/config');
const { ensureDbFile, reloadDbFromDisk } = require('./src/data-files');
const { errorHandler } = require('./src/errors');
const { rejectDelete, routerGate } = require('./src/routing');
const auth = require('./src/auth');
const technicianIncidents = require('./src/technician-incidents');
const operatorIncidents = require('./src/operator-incidents');
const uploads = require('./src/uploads');

const passToRouter = (req, res, next) => next();

/** Builds the Express app over `dbFile` (which must exist) and `uploadsDir`. */
function createApp({ dbFile = config.dbFile, uploadsDir = config.uploadsDir, logger = true } = {}) {
  uploads.ensureUploadsDir(uploadsDir);
  const app = jsonServer.create();
  const router = jsonServer.router(dbFile);
  const db = router.db;

  // CORS, request logging and no-cache headers. `static` points at server/public,
  // which doesn't exist, so json-server's bundled homepage isn't served.
  app.use(jsonServer.defaults({ logger, noGzip: true, static: path.join(__dirname, 'public') }));
  app.use(reloadDbFromDisk(db, dbFile));
  app.use(rejectDelete);
  app.use(jsonServer.bodyParser);

  // Public
  app.use(uploads.servePhotos(uploadsDir));
  app.post('/login', auth.login(db));

  // Everything below needs a valid bearer token.
  app.use(auth.requireAuth(db));
  app.post('/logout', auth.logout(db));
  app.post('/uploads', uploads.uploadPhoto(uploadsDir));

  // Incidents
  const technician = auth.requireRole('technician');
  const operator = auth.requireRole('operator');
  app.get('/incidents/summary', technician, technicianIncidents.summary(db));
  app.post('/incidents', technician, technicianIncidents.create(db));
  app.get('/incidents', operator, passToRouter);
  app.get('/incidents/:id', auth.byRole({ technician: technicianIncidents.show(db), operator: operatorIncidents.show(db) }));
  app.patch('/incidents/:id', auth.byRole({ technician: technicianIncidents.update(db), operator: operatorIncidents.update(db) }));
  app.get('/users', operator, operatorIncidents.users(db));

  // Catalog (any role) and the operator's incident list are served by
  // json-server's router; nothing else reaches it.
  app.use(
    routerGate({
      routerGets: [/^\/(categories|severities|sites|incidents)\/?$/],
      knownPaths: [/^\/(login|logout|categories|severities|sites|incidents|users|uploads)\/?$/, /^\/incidents\/[^/]+\/?$/],
    }),
  );
  app.use(router);
  app.use(errorHandler);
  return app;
}

if (require.main === module) {
  ensureDbFile(config.dbFile, config.seedFile);
  const app = createApp({ dbFile: config.dbFile, uploadsDir: config.uploadsDir });
  app.listen(config.port, () => {
    console.log(`Field Incident Reporter server on http://localhost:${config.port} (data: ${config.dbFile})`);
  });
}

module.exports = { createApp };
