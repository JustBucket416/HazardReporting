# Android: edit, withdraw and Status Change Notice

Status: ready-for-agent
Type: task
Blocked by: 11, 13

## What
- `IncidentInteractor.edit(localId, draft)`: bumps `localVersion`. A `Synced` row becomes `PendingUpdate`; a `PendingCreate` row stays `PendingCreate`.
- `IncidentInteractor.withdraw(localId)`: sets `PendingWithdraw`. The synchronizer follows the `createAttempted` rules in spec § 3.4.
- `dismissStatusChangeNotice(localId)`.
- `IncidentForm(localId)` in edit mode:
  - prefilled with the incident's values
  - keeps the original position
  - existing remote photos can be removed
- Detail screen ⋮ menu:
  - Edit opens the form
  - Withdraw… asks for confirmation, then withdraws and goes back
- Detail screen shows the Status Change Notice banner as soon as the notice arrives. The list's warning icon comes from the summary flag.

## Acceptance criteria
- [ ] Editing an Open incident offline and then reconnecting shows the change on the dashboard.
- [ ] Operator acknowledges, then the technician edits from a detail screen still showing Open: sync refuses the edit, the status updates, and the banner and list icon appear. Dismissing the notice clears both.
- [ ] Withdrawing a report that was never sent deletes it locally with no network call. Withdrawing a synced report shows it as Withdrawn on the dashboard.
- [ ] Interactor and synchronizer tests cover every `syncState` transition.

Refs: ADR-0004, ADR-0005
