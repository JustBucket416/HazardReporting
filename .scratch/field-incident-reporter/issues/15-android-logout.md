# Android: logout with sync-first and wipe

Status: ready-for-agent
Type: task
Blocked by: 13

## What
- `LoginInteractor.logout(force)` per spec § 3.2. It pushes first. If reports are still pending and `force` is false, it returns that count without wiping.
- The wipe runs in this exact order (spec § 3.4):
  1. Cancel the sync work and take the `Mutex`.
  2. Clear Room.
  3. Delete the photo files.
  4. Clear Coil's caches.
  5. Clear DataStore and `SessionHolder`.
  6. Call `/logout`, ignoring failure.
- The list's ⋮ → Log out runs the dialog flow from spec § 3.5:
  1. Confirm.
  2. Spinner with "Sending N pending reports…".
  3. If any are still pending: "N reports couldn't be sent and will be lost", with **Log out anyway** and **Cancel**.
  4. Go to Login with the back stack cleared.

## Acceptance criteria
- [ ] Logging out online with nothing pending wipes everything and returns to Login. Logging in as tech2 then shows none of tech1's data.
- [ ] Logging out offline with 1 pending report shows the warning. Cancel keeps everything; **Log out anyway** wipes it.
- [ ] Interactor tests check the wipe order with `coVerifyOrder`.

Refs: ADR-0007
