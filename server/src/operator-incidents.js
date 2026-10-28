// Operator incident routes (spec § 1 "Operator routes", ADR-0004).
// GET /incidents itself is served by json-server's router (filters, q, sort, paging).
const { sendError } = require('./errors');
const { publicUser } = require('./auth');
const { unknownFields, isPlainObject } = require('./validation');

const STATUSES = ['open', 'acknowledged', 'in_progress', 'resolved', 'rejected', 'withdrawn'];
const PRIORITIES = ['low', 'medium', 'high', 'urgent'];
const FINAL_STATUSES = ['resolved', 'rejected', 'withdrawn'];
const TRANSITIONS = {
  open: ['acknowledged', 'rejected'],
  acknowledged: ['in_progress', 'rejected'],
  in_progress: ['resolved', 'rejected'],
};

/** GET /incidents/:id for an operator: the full incident. */
function show(db) {
  return (req, res) => {
    const incident = db.get('incidents').find({ id: req.params.id }).value();
    if (!incident) return sendError(res, 404, 'Incident not found');
    res.json(incident);
  };
}

/** Body checks for an operator PATCH; returns `{ changes, noteText }` or `{ fields }`. */
function parsePatch(body) {
  if (!isPlainObject(body) || Object.keys(body).length === 0) {
    return { fields: { body: 'Send any of status, priority, assignee, note' } };
  }
  const fields = unknownFields(body, ['status', 'priority', 'assignee', 'note']);
  const changes = {};
  let noteText;

  if ('status' in body) {
    if (STATUSES.includes(body.status)) changes.status = body.status;
    else fields.status = `Must be one of ${STATUSES.join(', ')}`;
  }
  if ('priority' in body) {
    if (body.priority === null || PRIORITIES.includes(body.priority)) changes.priority = body.priority;
    else fields.priority = `Must be one of ${PRIORITIES.join(', ')} or null`;
  }
  if ('assignee' in body) {
    // A blank assignee (e.g. a cleared text field) means "nobody".
    if (body.assignee === null) changes.assignee = null;
    else if (typeof body.assignee === 'string') changes.assignee = body.assignee.trim() || null;
    else fields.assignee = 'Must be a string or null';
  }
  if ('note' in body) {
    if (typeof body.note === 'string' && body.note.trim()) noteText = body.note.trim();
    else fields.note = 'Must be a non-empty string';
  }
  return Object.keys(fields).length ? { fields } : { changes, noteText };
}

/**
 * PATCH /incidents/:id for an operator: status (per the transition table),
 * priority, assignee, and `note`, which is appended to `notes`. Final
 * incidents are read-only (409).
 */
function update(db) {
  return (req, res) => {
    const incident = db.get('incidents').find({ id: req.params.id }).value();
    if (!incident) return sendError(res, 404, 'Incident not found');

    const { changes, noteText, fields } = parsePatch(req.body);
    if (fields) return sendError(res, 400, 'Validation failed', fields);

    if (FINAL_STATUSES.includes(incident.status)) {
      return sendError(res, 409, `Incident is ${incident.status} and can no longer be changed`);
    }
    if (changes.status && changes.status !== incident.status && !TRANSITIONS[incident.status].includes(changes.status)) {
      return sendError(res, 409, `Cannot change status from ${incident.status} to ${changes.status}`);
    }

    const now = Date.now();
    if (noteText) changes.notes = [...incident.notes, { at: now, byUserId: req.user.id, text: noteText }];
    const updated = db
      .get('incidents')
      .find({ id: incident.id })
      .assign({ ...changes, updatedAt: now })
      .write();
    res.json(updated);
  };
}

/** GET /users: every user without `password`, so the dashboard can name reporters. */
function users(db) {
  return (req, res) => {
    res.json(db.get('users').map(publicUser).value());
  };
}

module.exports = { show, update, users, TRANSITIONS };
