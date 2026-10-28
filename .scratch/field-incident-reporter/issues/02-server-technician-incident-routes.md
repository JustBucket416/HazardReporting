# Server: technician incident routes

Status: ready-for-agent
Type: task
Blocked by: 01

## What
Technician side of `incidents`, per spec § 1 "Technician routes":
- `GET /incidents/summary`.
- `GET /incidents/:id`, with `assignee` and `notes` stripped.
- `POST /incidents`, with dedupe on `reporterId + createdAt`. Both the first create and a duplicate return 200.
- `PATCH /incidents/:id`, accepting only report fields or `withdrawn`; 409 when the incident is not open.
- Validation errors return 400 with a `fields` object.
- `updatedAt` and `receivedAt` stamping.

## Acceptance criteria
- [ ] tech1 never sees tech2's incidents. GET or PATCH on one of tech2's incidents returns 404.
- [ ] Withdrawn incidents are missing from the summary.
- [ ] Re-POSTing with the same `createdAt` leaves exactly one incident, updated to the latest body. Both calls return 200 with the same `id`.
- [ ] Two POSTs with the same title but different `createdAt` create two incidents.
- [ ] PATCH on an `acknowledged` incident returns 409. PATCH that includes `priority` returns 400.
- [ ] The technician view never contains `assignee` or `notes`.

Refs: ADR-0003, ADR-0004, ADR-0005
