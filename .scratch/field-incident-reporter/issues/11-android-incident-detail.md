# Android: incident detail (on-demand details)

Status: ready-for-agent
Type: task
Blocked by: 10

## What
- `IncidentInteractor.observeIncident(localId)` and `loadIncidentDetails(localId, force)`. The repository fetches only when `force || !detailsLoaded`.
- Coil 3 wired to the app's `OkHttpClient`. The converter resolves relative photo URLs.
- `IncidentDetailRoute`/`Screen` per spec § 3.5 "Incident detail":
  - a photo pager, each image shimmering until it loads
  - all fields except assignee, with the date shown in absolute form
  - details that haven't loaded shimmer; if loading fails they become "Details not downloaded" with Retry
  - pull-to-refresh calls with `force = true`
  - a "Waiting to send" indicator
- A ⋮ menu shown only when details are loaded and the status is Open. Its actions do nothing until issue 14.

## Acceptance criteria
- [ ] Opening a server incident for the first time fetches its details once. Reopening it makes no network call; pull-to-refresh does.
- [ ] Opening an incident offline whose details were never fetched shows the summary, "Details not downloaded", and no ⋮ menu.
- [ ] Repository tests cover every combination of `force` and `detailsLoaded`.

Refs: ADR-0006

## Comments

- Implemented on feat/android-app in f9d5423; unit tests green.
