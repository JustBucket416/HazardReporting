# Android: incident list (summaries)

Status: ready-for-agent
Type: task
Blocked by: 08, 09

## What
- Room `incidents` and `incident_photos` entities with the **full** schema from spec § 3.3. This issue only fills the summary fields.
- `IncidentLocalDataSource`, `IncidentRemoteDataSource` and `IncidentRepository`.
- `IncidentInteractor.observeMyIncidents()` and `refreshIncidents()`. `refreshIncidents()` only pulls for now; issue 13 adds the push.
- Pull rule: rows with a pending change keep their report fields; only `status` and `updatedAt` are updated.
- Login triggers `refreshIncidents()`.
- `IncidentListRoute`/`Screen`, with these parts of spec § 3.5 "Incident list":
  - skeleton rows on the first load
  - the empty state
  - the error card
  - the snackbar when a refresh fails
  - pull-to-refresh
  - rows with chips and the pending and notice icons
  - the extended FAB
- Tapping a row opens `IncidentDetail(localId)`.
- Not in this issue: logout (issue 15) and the session-invalid banner (issue 13).

## Acceptance criteria
- [ ] tech1 sees only their own non-withdrawn incidents, newest first.
- [ ] Pull-to-refresh while offline keeps the rows and shows the snackbar.
- [ ] The first login with the server down shows the error card, and Retry works once it's back.
- [ ] Repository tests cover the pull rule on pending rows. ViewModel tests cover every state.

Refs: ADR-0006

## Comments

- Implemented on feat/android-app in 4aa5c9e; unit tests green.
