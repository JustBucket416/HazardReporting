// Validation of the report fields a technician sends (spec § 1, "Validation").
// Every function returns `{ values, fields }`: normalised values to store, and
// a `fields` map of per-field messages (empty when valid).

const REPORT_FIELDS = [
  'title',
  'description',
  'categoryId',
  'severityId',
  'siteId',
  'locationDescription',
  'position',
  'photoUrls',
];

const TITLE_MAX = 80;
const DESCRIPTION_MAX = 1000;
const PHOTOS_MIN = 1;
const PHOTOS_MAX = 3;

const isFiniteNumber = (v) => typeof v === 'number' && Number.isFinite(v);

function checkPosition(p) {
  if (p === null) return { value: null };
  if (typeof p !== 'object' || Array.isArray(p)) return { error: 'Must be { lat, lng, accuracyM } or null' };
  const { lat, lng, accuracyM } = p;
  if (!isFiniteNumber(lat) || lat < -90 || lat > 90) return { error: 'lat must be a number between -90 and 90' };
  if (!isFiniteNumber(lng) || lng < -180 || lng > 180) return { error: 'lng must be a number between -180 and 180' };
  if (!isFiniteNumber(accuracyM) || accuracyM < 0) return { error: 'accuracyM must be a non-negative number' };
  return { value: { lat, lng, accuracyM } };
}

/** Checks one report field; returns `{ value }` or `{ error }`. */
function checkField(name, value, db) {
  switch (name) {
    case 'title':
      if (typeof value !== 'string') return { error: 'Required' };
      if (value.trim().length < 1) return { error: 'Required' };
      if (value.trim().length > TITLE_MAX) return { error: `At most ${TITLE_MAX} characters` };
      return { value };
    case 'description':
      if (typeof value !== 'string') return { error: 'Must be a string' };
      if (value.length > DESCRIPTION_MAX) return { error: `At most ${DESCRIPTION_MAX} characters` };
      return { value };
    case 'categoryId':
    case 'severityId':
    case 'siteId': {
      const collection = { categoryId: 'categories', severityId: 'severities', siteId: 'sites' }[name];
      if (typeof value !== 'string' || !db.get(collection).find({ id: value }).value()) {
        return { error: 'Unknown id' };
      }
      return { value };
    }
    case 'locationDescription':
      if (typeof value !== 'string') return { error: 'Must be a string' };
      return { value };
    case 'position':
      return checkPosition(value);
    case 'photoUrls':
      if (!Array.isArray(value) || value.length < PHOTOS_MIN || value.length > PHOTOS_MAX) {
        return { error: `Must have ${PHOTOS_MIN}–${PHOTOS_MAX} photos` };
      }
      if (!value.every((u) => typeof u === 'string' && u.length > 0)) return { error: 'Each photo URL must be a non-empty string' };
      return { value: [...value] };
    default:
      return { error: 'Not allowed' };
  }
}

/** Fields not in `allowed` become "Not allowed" errors. */
function unknownFields(body, allowed) {
  const fields = {};
  for (const key of Object.keys(body)) if (!allowed.includes(key)) fields[key] = 'Not allowed';
  return fields;
}

/**
 * POST /incidents body. `description` and `locationDescription` default to ""
 * and `position` to null when omitted; everything else is required.
 */
function validateCreate(body, db) {
  const fields = unknownFields(body, [...REPORT_FIELDS, 'createdAt']);
  const input = { description: '', locationDescription: '', position: null, ...body };
  const values = {};
  for (const name of REPORT_FIELDS) {
    const r = checkField(name, input[name], db);
    if (r.error) fields[name] = r.error;
    else values[name] = r.value;
  }
  if (!Number.isSafeInteger(body.createdAt) || body.createdAt <= 0) {
    fields.createdAt = 'Must be epoch milliseconds (integer)';
  } else {
    values.createdAt = body.createdAt;
  }
  return { values, fields };
}

/** Technician PATCH carrying report fields: only those present are checked. */
function validateReportPatch(body, db) {
  const fields = unknownFields(body, REPORT_FIELDS);
  const values = {};
  for (const name of Object.keys(body)) {
    if (fields[name]) continue;
    const r = checkField(name, body[name], db);
    if (r.error) fields[name] = r.error;
    else values[name] = r.value;
  }
  return { values, fields };
}

const isPlainObject = (v) => typeof v === 'object' && v !== null && !Array.isArray(v);

module.exports = { REPORT_FIELDS, validateCreate, validateReportPatch, unknownFields, isPlainObject };
