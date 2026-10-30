# HazardReporting — Field Incident Reporter

A prototype facility-maintenance system:

- **Technicians** use an offline-first Android app to report incidents with photos, view the incidents they reported, and edit or withdraw them while they are Open.
- **Operators** use a Next.js web dashboard to triage, filter and resolve every incident.
- A local **json-server** is the backend for both.

| Folder | What it is |
|---|---|
| [`server/`](server/) | json-server 0.17 inside a custom `server.js`: token auth, technician/operator rules, photo uploads |
| [`dashboard/`](dashboard/) | Operator dashboard: Next.js App Router, TypeScript, Tailwind, shadcn/ui |
| [`android/`](android/) | Technician app: Kotlin, Jetpack Compose, Dagger 2, Room, Retrofit, WorkManager |

The domain language is in [GLOSSARY.md](GLOSSARY.md), the decisions and their reasons are in [docs/adr/](docs/adr/), and the full spec with its issues is in [.scratch/field-incident-reporter/](.scratch/field-incident-reporter/).

## Prerequisites

- Node.js LTS (20 or newer) and npm
- Android Studio with an Android emulator (API 26 or newer)

## Run it

1. **Server** (port 3001), from the repo root:

   Linux / macOS:
   ```bash
   cd server && npm install && npm run reset && npm run dev
   ```

   Windows (PowerShell):
   ```powershell
   cd server; npm install; if ($?) { npm run reset; if ($?) { npm run dev } }
   ```

   `npm run reset` restores the seed data, deletes uploaded photos and signs everyone out. `npm test` runs the server's test suite.

2. **Dashboard** (port 3000), in a second terminal:
   ```bash
   cd dashboard
   npm install
   npm run dev
   ```
   Open http://localhost:3000.

3. **Android app**: open `android/` in Android Studio and run the `app` configuration on an **emulator**. The app reaches the server at `http://10.0.2.2:3001/`, which is how an emulator reaches the computer it runs on. On a physical device, change the `BASE_URL` constant in `android/app/src/main/java/com/example/hazardreporting/data/remote/RetrofitFactory.kt` to your computer's LAN IP (for example `http://192.168.1.20:3001/`) and add that host to `android/app/src/debug/res/xml/network_security_config.xml`.

## Demo accounts

All passwords are `password`.

| Email | Role | Use it in |
|---|---|---|
| `tech1@example.com` | Technician | Android app |
| `tech2@example.com` | Technician | Android app |
| `ops@example.com` | Operator | Dashboard |

The app accepts technicians only, and the dashboard accepts operators only.

## Demo script

1. Start the server with `npm run reset && npm run dev`, start the dashboard, and run the app on an emulator.
2. Sign in to the app as tech1. You see only tech1's incidents, and Withdrawn ones are hidden.
3. Turn on airplane mode and report an incident with two photos. It appears at once with a pending icon. Turn airplane mode off: within a minute it is sent, and the dashboard shows exactly one copy.
4. Open that incident in the app. On the dashboard, as ops, acknowledge it. Back in the app, go offline and edit it from the detail screen that still shows Open. When you reconnect, the edit is refused, the incident shows Acknowledged, and a Status Change Notice appears.
5. Withdraw an Open incident in the app. It disappears from the app and shows as Withdrawn on the dashboard.
6. On the dashboard, filter by severity Critical and status Open, add a note to an incident and resolve one. In the app, pull to refresh: the incident shows Resolved, but never the note or the assignee.
7. Go offline, report another incident, and log out. The app warns that 1 report will be lost before it wipes the device.

## Android builds on Windows

If Gradle fails with `Unable to establish loopback connection`, point Java at a temp folder without a short (8.3) path name, for example:

```powershell
$env:JAVA_TOOL_OPTIONS = "-Djdk.net.unixdomain.tmpdir=F:\tmp"
```

If KSP fails with an incremental-cache error about "different roots" (Gradle cache and project on different drives), delete `android/app/build/kspCaches` and build again.
