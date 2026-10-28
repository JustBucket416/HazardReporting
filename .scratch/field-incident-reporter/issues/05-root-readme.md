# Root README

Status: ready-for-agent
Type: task
Blocked by: 01, 16

## What
Root `README.md` covering:
- What the system is.
- Prerequisites: Node LTS, Android Studio, and an emulator.
- How to start the server (`reset`, `dev`), the dashboard and the app.
- The emulator reaches the server at `10.0.2.2:3001`. On a physical device, change the `BASE_URL` constant to the machine's LAN IP.
- The demo accounts.
- The acceptance demo script from the spec.
- Links to `GLOSSARY.md` and `docs/adr/`.

## Acceptance criteria
- [ ] Someone starting from a fresh clone can follow the README through to the end of the demo script.

Refs: spec "Acceptance"
