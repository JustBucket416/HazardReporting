# Server: operator routes and status transitions

Status: ready-for-agent
Type: task
Blocked by: 01

## What
- Operator `GET /incidents`:
  - json-server filters, `q`, sort and paging
  - the `X-Total-Count` header
  - `createdAt_gte` / `createdAt_lte`
- Operator `GET /incidents/:id`.
- Operator `PATCH /incidents/:id` taking `{status, priority, assignee, note}`:
  - status changes follow the transition table, 409 otherwise
  - incidents in a final status are read-only (409)
  - `note` is appended to `notes` as `{at, byUserId, text}`
- `GET /users`, without passwords.
- 403 for a technician calling these routes, and for an operator calling `/incidents/summary`.

## Acceptance criteria
- [ ] open → acknowledged succeeds; open → resolved returns 409.
- [ ] Any change to a resolved incident returns 409.
- [ ] Two added notes are both kept, in order, with server timestamps.
- [ ] `?status=open&severityId=<critical>&_page=1&_limit=10` filters correctly and sets `X-Total-Count`.
- [ ] `GET /users` never includes `password`.

Refs: spec § 1, ADR-0004
