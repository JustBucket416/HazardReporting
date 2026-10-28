# json-server 0.17 wrapped in a custom server.js that enforces the rules

The backend is a local json-server, but plain json-server has no authentication and trusts every write. We run json-server **0.17** as a module inside our own `server.js`. That lets us add middleware: opaque bearer tokens kept in a `sessions` collection, role-based access, technician ownership checks, field-owned PATCH rules, valid status transitions, create dedupe, and a `POST /uploads` route for photos. json-server v1 drops the middleware API, which is why we stay on 0.17. Passwords are plain text in the seed and tokens never expire, because this is a local prototype.

## Considered Options

- **Fake auth, where the client just picks a user id**: rejected once the technician's list had to be scoped by the caller's token.
- **json-server-auth with JWTs**: brings a secret, expiry and refresh handling on Android for no demo value.
- **Next.js as a backend-for-frontend in front of an open json-server**: doesn't protect the Android client's traffic.

## Consequences

- `npm run reset` wipes `sessions`, so a phone can hold a dead token. The app treats this as a 401 and shows a banner; it never logs out automatically.
- Upload responses return **relative** URLs (`/uploads/<name>`), so stored incidents stay valid whichever host a client uses to reach the server (emulator `10.0.2.2`, a LAN IP, or `localhost`).
- For technicians, the server strips operator-only fields (`assignee`, `notes`) from responses.
