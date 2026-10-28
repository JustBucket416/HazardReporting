# Dashboard: scaffold and operator auth

Status: ready-for-agent
Type: task
Blocked by: 01

## What
- `dashboard/` set up with the Next.js App Router, TypeScript, Tailwind and shadcn/ui.
- A server-side API helper that reads the token from the httpOnly cookie and sends it as a `Bearer` header.
- `/login`, open to operators only.
- `middleware.ts` that redirects to `/login` when there is no cookie.
- A 401 from the server clears the cookie and redirects to `/login`.
- Logout.

## Acceptance criteria
- [ ] ops logs in and reaches `/incidents`; tech1 gets "Operators only".
- [ ] After the server is reset (`npm run reset`), the next navigation redirects to login.

Refs: spec § 2
