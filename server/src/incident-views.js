// What each role gets to see of an incident.

/** Technician view: everything except the operator-only `assignee` and `notes` (ADR-0001). */
function technicianView(incident) {
  const { assignee, notes, ...rest } = incident;
  return rest;
}

/** Incident Summary, as listed by GET /incidents/summary. */
function summaryView({ id, title, categoryId, severityId, status, createdAt, updatedAt }) {
  return { id, title, categoryId, severityId, status, createdAt, updatedAt };
}

module.exports = { technicianView, summaryView };
