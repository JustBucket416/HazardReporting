# Server: scaffold, seed, auth and catalog

Status: ready-for-agent
Type: task
Blocked by: —

## What
Create `server/` with:
- `package.json` (npm)
- `server.js`, running json-server **0.17** as a module
- `seed.json`, a `db.json` copy, and `uploads/seed/`
- `npm run dev` (port 3001) and `npm run reset`
- Seed data per spec § 1, with placeholder seed photos
- `POST /login`, `POST /logout`, and middleware that checks the bearer token (401 when it's missing or unknown); `server.js` generates the ids
- `GET /categories|severities|sites`
- The shared 4xx error body
- `DELETE` returns 405 on every route
- A skeleton 403 role guard

## Acceptance criteria
- [ ] `npm run reset && npm run dev` serves on :3001.
- [ ] tech1 login returns a token and creates a `sessions` row; a wrong password returns 401.
- [ ] Catalog routes return 401 without a token and 200 with one.
- [ ] `/logout` deletes the session; the old token then gets 401.
- [ ] `reset` invalidates all tokens.

Refs: spec § 1, ADR-0001
