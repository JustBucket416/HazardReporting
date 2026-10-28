// Technician incident routes (spec § 1 "Technician routes", ADR-0003/0004/0005).
// A technician only ever sees and changes the incidents they reported.
const { randomUUID } = require('crypto');
const { sendError } = require('./errors');
const { technicianView, summaryView } = require('./incident-views');
const { REPORT_FIELDS, validateCreate, validateReportPatch, isPlainObject } = require('./validation');

const pick = (obj, keys) => Object.fromEntries(keys.filter((k) => k in obj).map((k) => [k, obj[k]]));

/** The caller's own incident, or undefined (other people's incidents are reported as missing). */
function findOwn(db, req) {
  const incident = db.get('incidents').find({ id: req.params.id }).value();
  return incident && incident.reporterId === req.user.id ? incident : undefined;
}

/** GET /incidents/summary: own incidents, no withdrawn ones, newest createdAt first. */
function summary(db) {
  return (req, res) => {
    const items = db
      .get('incidents')
      .filter((i) => i.reporterId === req.user.id && i.status !== 'withdrawn')
      .orderBy(['createdAt'], ['desc'])
      .map(summaryView)
      .value();
    res.json(items);
  };
}

/** GET /incidents/:id for a technician: own incident without assignee/notes, else 404. */
function show(db) {
  return (req, res) => {
    const incident = findOwn(db, req);
    if (!incident) return sendError(res, 404, 'Incident not found');
    res.json(technicianView(incident));
  };
}

/**
 * POST /incidents: report an incident. Deduplicated on reporter + createdAt
 * (ADR-0003): a retry updates the existing incident's report fields, as long
 * as it is still open (the server wins otherwise). Both cases answer 200.
 */
function create(db) {
  return (req, res) => {
    if (!isPlainObject(req.body)) return sendError(res, 400, 'Body must be a JSON object');
    const { values, fields } = validateCreate(req.body, db);
    if (Object.keys(fields).length) return sendError(res, 400, 'Validation failed', fields);

    const now = Date.now();
    const incidents = db.get('incidents');
    const existing = incidents.find({ reporterId: req.user.id, createdAt: values.createdAt });
    if (existing.value()) {
      if (existing.value().status === 'open') {
        existing.assign({ ...pick(values, REPORT_FIELDS), updatedAt: now }).write();
      }
      return res.status(200).json(technicianView(existing.value()));
    }

    const incident = {
      id: randomUUID(),
      reporterId: req.user.id,
      ...pick(values, REPORT_FIELDS),
      status: 'open',
      priority: null,
      assignee: null,
      notes: [],
      createdAt: values.createdAt,
      receivedAt: now,
      updatedAt: now,
    };
    incidents.push(incident).write();
    res.status(200).json(technicianView(incident));
  };
}

/** Body checks for a technician PATCH; returns `{ changes }` or `{ fields }`. */
function parsePatch(body, db) {
  if (!isPlainObject(body) || Object.keys(body).length === 0) {
    return { fields: { body: 'Send report fields or { status: "withdrawn" }' } };
  }
  if ('status' in body) {
    const fields = {};
    if (body.status !== 'withdrawn') fields.status = 'Only "withdrawn" is allowed';
    for (const key of Object.keys(body)) if (key !== 'status') fields[key] = 'Cannot be combined with status';
    return Object.keys(fields).length ? { fields } : { changes: { status: 'withdrawn' } };
  }
  const { values, fields } = validateReportPatch(body, db);
  return Object.keys(fields).length ? { fields } : { changes: values };
}

/** PATCH /incidents/:id for a technician: report fields or withdraw, only while open. */
function update(db) {
  return (req, res) => {
    const incident = findOwn(db, req);
    if (!incident) return sendError(res, 404, 'Incident not found');

    const { changes, fields } = parsePatch(req.body, db);
    if (fields) return sendError(res, 400, 'Validation failed', fields);
    if (incident.status !== 'open') {
      return sendError(res, 409, `Incident is ${incident.status}; only open incidents can be changed`);
    }

    const updated = db
      .get('incidents')
      .find({ id: incident.id })
      .assign({ ...changes, updatedAt: Date.now() })
      .write();
    res.json(technicianView(updated));
  };
}

module.exports = { summary, show, create, update };
