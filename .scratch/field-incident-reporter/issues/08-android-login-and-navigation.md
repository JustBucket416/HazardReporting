# Android: login screen, navigation shell, splash

Status: ready-for-agent
Type: task
Blocked by: 07

## What
- `LoginInteractor.login` and `currentUser()`.
- Type-safe NavHost with `Login`, `IncidentList`, `IncidentDetail` and `IncidentForm`. The non-login screens can be placeholders until their issues land.
- Splash screen held until `currentUser()` decides the start destination.
- `LoginRoute`/`LoginScreen` and `LoginViewModel`, matching spec § 3.5 "Login":
  - the button ignores taps and shows a shimmering "Signing in…" while the request runs
  - errors show inline
  - submitting from the IME works
- Shared ViewModel exception handler: logs through `Logger`, then calls `onUnexpected`.

## Acceptance criteria
- [ ] tech1 login against the running server lands on `IncidentList`, and Back doesn't return to Login.
- [ ] After a kill and relaunch the app goes straight to `IncidentList`, with no Login flash.
- [ ] Wrong password and airplane mode each show the correct inline error.
- [ ] `LoginViewModel` tests cover success, invalid credentials, no connection, and an unexpected exception (it is logged and loading is cleared).

Refs: spec § 3.5
