// The shared 4xx error body: { error, fields? }.

function sendError(res, status, error, fields) {
  const body = { error };
  if (fields && Object.keys(fields).length > 0) body.fields = fields;
  res.status(status).json(body);
}

/** Final Express error handler: malformed JSON and similar client errors become 4xx bodies. */
function errorHandler(err, req, res, next) {
  if (res.headersSent) return next(err);
  if (err.type === 'entity.parse.failed') {
    return sendError(res, 400, 'Malformed JSON body');
  }
  const status = err.status || err.statusCode;
  if (status >= 400 && status < 500) {
    return sendError(res, status, err.expose ? err.message : 'Bad request');
  }
  console.error(err);
  res.status(500).json({ error: 'Internal server error' });
}

module.exports = { sendError, errorHandler };
