# Offline-first technician app with server-generated ids

Technicians work in basements, plant rooms and remote sites with poor signal, so reporting, editing and withdrawing incidents must work with no connection. Room is the app's source of truth. Each of those actions only writes to Room and enqueues a WorkManager push; the app never waits on the network to save.

The server, not the client, generates incident ids. So every local row has its own `localId`, and the `serverId` stays null until the first successful POST. Navigation and the UI address incidents by `localId` only, so the server id can arrive without anything changing for the user.

## Consequences

- One `IncidentSynchronizer` (in `data/sync`) does every push, guarded by a `Mutex`. The work is unique: a one-shot run with `KEEP` and a network constraint, plus a periodic run every 15 minutes, both with exponential backoff.
- Photos upload first, and each photo row stores its `remoteUrl` the moment its upload succeeds, so a retry never re-uploads a photo that already arrived.
- Every local edit increments `localVersion`. A push marks the row synced only if `localVersion` is still what it sent, so edits made during an in-flight push are not lost.
- Background work only pushes. Pulling from the server happens only after login and when the user pulls to refresh.
- A pull never overwrites the report fields of a row that has a pending change.
