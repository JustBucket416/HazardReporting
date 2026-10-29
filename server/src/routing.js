// Cross-cutting routing rules: no DELETE anywhere, and what reaches json-server's router.
const { sendError } = require('./errors');

/** DELETE is disabled for everyone (ADR-0005): technicians withdraw instead. */
function rejectDelete(req, res, next) {
  if (req.method === 'DELETE') return sendError(res, 405, 'DELETE is not supported; withdraw the incident instead');
  next();
}

/**
 * Lets only explicitly allowed GETs through to json-server's router (so `/db`,
 * `/sessions`, raw writes and the rest of its generated routes stay closed).
 * Anything else is 405 for a known path with the wrong method, else 404.
 */
function routerGate({ routerGets, knownPaths }) {
  return (req, res, next) => {
    if (req.method === 'GET' && routerGets.some((re) => re.test(req.path))) return next();
    if (knownPaths.some((re) => re.test(req.path))) return sendError(res, 405, 'Method not allowed');
    sendError(res, 404, 'Not found');
  };
}

module.exports = { rejectDelete, routerGate };
