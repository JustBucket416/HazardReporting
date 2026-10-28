# Android: report form (create)

Status: ready-for-agent
Type: task
Blocked by: 09, 10

## What
- `IncidentInteractor.report(draft)`:
  - validates the draft and throws `ValidationException(fields)` on failure
  - writes a `PendingCreate` row and its photo rows, with `createdAt` set to the current epoch ms
  - calls a sync-enqueue hook that issue 13 implements
- `LocationProvider`: the interface in domain and a fused implementation in data. It times out after 10 s and falls back to the last known location only if it is under 2 min old.
- Photo capture:
  - `TakePicture` through a `FileProvider` into `files/photos/`
  - `PickVisualMedia`, copying the picked image into `files/photos/` straight away
- `IncidentFormRoute`/`Screen` in create mode, per spec § 3.5 "Report / edit form":
  - a photo strip with a bottom sheet for camera or gallery
  - dropdowns, and segmented buttons for severity
  - the position line with Re-capture
  - field errors appear only after the first Save or once a field loses focus
  - a "Discard changes?" dialog
  - `SavedStateHandle` holds all form state and the pending camera `Uri`
  - Save goes back with a "saved" result, and the list shows a "Report saved" snackbar
- The list FAB opens `IncidentForm()`.

## Acceptance criteria
- [ ] Saving with no photos or no site shows inline errors and writes nothing.
- [ ] Reporting offline with 2 photos makes the row appear immediately with a pending icon.
- [ ] With "Don't keep activities" on, taking a photo restores the half-filled form and the new photo.
- [ ] With location permission denied, the report still saves, without a position.
- [ ] Interactor validation tests cover every rule.

Refs: spec § 3.2, § 3.5
