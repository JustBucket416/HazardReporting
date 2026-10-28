# Dashboard: incident detail and triage

Status: ready-for-agent
Type: task
Blocked by: 04, 17

## What
The `/incidents/[id]` page:
- A photo gallery built from `photoUrls`.
- A Leaflet + OpenStreetMap map when `position` is set, rendered on the client only.
- All of the incident's fields.
- A triage panel:
  - a status select offering only the valid transitions
  - priority and assignee
  - append-only notes with an "Add note" box
- Each triage change goes through a Server Action, then `revalidatePath`.
- Incidents in a final status are read-only.
- On a 409 the page shows the server's message.

## Acceptance criteria
- [ ] Every transition in the table works, and invalid transitions are never offered.
- [ ] A new note appears with the operator's name and the time.
- [ ] A Withdrawn incident is fully read-only.

Refs: spec § 2, ADR-0004
