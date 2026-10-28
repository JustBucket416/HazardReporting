# Android: push sync

Status: ready-for-agent
Type: task
Blocked by: 02, 04, 12

## What
- `data/sync/IncidentSynchronizer.push()`, exactly as spec § 3.4:
  - runs under a `Mutex`
  - uploads photos first and stores each `remoteUrl` as soon as it succeeds
  - sets `createAttempted`, then POSTs and stores the `serverId`
  - marks a row `Synced` only if `localVersion` hasn't changed since the push read it
  - on 409, refetches the incident, overwrites the local row and sets the notice flag
  - on `NoConnectionException`, retries later
  - on `UnauthorizedException`, stops and sets the session-invalid flag
- `SyncWorker`, built through a Dagger `WorkerFactory`, with the default WorkManager initializer removed in the manifest.
- Work scheduling:
  - unique work `sync-once` (KEEP, CONNECTED, exponential backoff), enqueued by `report`, `edit` and `withdraw`
  - periodic work `sync-periodic` every 15 min
- `refreshIncidents()` now pushes, then pulls.
- The session-invalid banner on the list.
- Each local photo file is deleted after its upload.

## Acceptance criteria
- [ ] An incident reported offline syncs once the connection returns, and the dashboard and server show exactly one copy.
- [ ] Synchronizer unit tests cover:
  - a partial photo-upload failure, then a retry that doesn't re-upload the photos already sent
  - a lost POST response, then a retry: still one server incident, and its `serverId` is adopted
  - an edit made during an in-flight PATCH stays pending
  - 409 → refetch and notice
  - 401 → stop and set the flag
  - `NoConnectionException` → retry
- [ ] Running `npm run reset` while the app holds a token shows the banner, and no data is lost.

Refs: ADR-0002, ADR-0003, ADR-0004
