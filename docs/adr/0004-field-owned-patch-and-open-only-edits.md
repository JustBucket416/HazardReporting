# Field-owned PATCH, technician edits only while Open, server wins

Technicians and operators write different fields of the same incident, and json-server's `PUT` replaces the whole document. So clients only ever `PATCH`, and only the fields they own:
- Technicians: the report fields, or `status: Withdrawn`.
- Operators: status, priority, assignee and notes.

A technician may edit or withdraw an incident only while it is Open, and the server enforces this with `409 Conflict`. That means a conflict can only happen when a technician's pending change meets an operator who has already moved the incident on.

The server always wins. On a 409 the synchronizer drops the pending change, refetches the incident, and raises a Status Change Notice inside the app. There is no merge UI and there are no system notifications.

## Consequences

- There is no `Conflict` sync state. After a 409 the row is synced and carries a notice flag, which the user dismisses.
- A cached incident can still show Edit/Withdraw for an incident that is no longer Open (see ADR-0006). The 409 path is the safety net for that staleness.
