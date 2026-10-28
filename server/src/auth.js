// Opaque bearer tokens stored in the `sessions` collection, plus the role guard.
const { randomUUID } = require('crypto');
const { sendError } = require('./errors');

function publicUser(user) {
  const { password, ...rest } = user;
  return rest;
}

/** POST /login: { email, password } -> 200 { token, user } and a new session row, or 401. */
function login(db) {
  return (req, res) => {
    const { email, password } = req.body || {};
    const user =
      typeof email === 'string' && typeof password === 'string'
        ? db
            .get('users')
            .find((u) => u.email.toLowerCase() === email.trim().toLowerCase() && u.password === password)
            .value()
        : undefined;
    if (!user) return sendError(res, 401, 'Invalid email or password');

    const session = { id: randomUUID(), userId: user.id, createdAt: Date.now() };
    db.get('sessions').push(session).write();
    res.status(200).json({ token: session.id, user: publicUser(user) });
  };
}

/** POST /logout (after requireAuth): deletes the caller's session, 204. */
function logout(db) {
  return (req, res) => {
    db.get('sessions').remove({ id: req.token }).write();
    res.status(204).end();
  };
}

/** Resolves `Authorization: Bearer <token>` to `req.user`; 401 when missing or unknown. */
function requireAuth(db) {
  return (req, res, next) => {
    const match = /^Bearer\s+(\S+)$/i.exec(req.get('Authorization') || '');
    const token = match && match[1];
    const session = token && db.get('sessions').find({ id: token }).value();
    const user = session && db.get('users').find({ id: session.userId }).value();
    if (!user) return sendError(res, 401, 'Missing or invalid token');
    req.token = token;
    req.user = publicUser(user);
    next();
  };
}

/** 403 unless the authenticated caller has one of `roles`. */
function requireRole(...roles) {
  return (req, res, next) => {
    if (!req.user || !roles.includes(req.user.role)) {
      return sendError(res, 403, `This route is for ${roles.join(' or ')} accounts only`);
    }
    next();
  };
}

module.exports = { login, logout, requireAuth, requireRole, publicUser };
