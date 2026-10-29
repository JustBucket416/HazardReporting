# Incident Dashboard

The operators' web dashboard for the Field Incident Reporter (spec § 2): sign in, filter and triage incidents.

Next.js (App Router) + TypeScript + Tailwind + shadcn/ui. Server Components read from the json-server backend; Server Actions write to it.

## Run

1. Start the server: `npm run reset && npm run dev` in `server/` (port 3001).
2. Here: `npm install`, then `npm run dev` and open http://localhost:3000.
3. Sign in as `ops@example.com` / `password`. Technician accounts are refused ("Operators only").

The backend URL defaults to `http://localhost:3001`; set `API_BASE_URL` to point elsewhere.
