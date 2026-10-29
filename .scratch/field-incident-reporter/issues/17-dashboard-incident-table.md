# Dashboard: incident table, filters, summary strip

Status: ready-for-agent
Type: task
Blocked by: 03, 16

## What
The `/incidents` page, per spec § 2:
- The table columns, with reporter and site names resolved.
- Filters: status, severity, priority, category, site, assignee, date range, and free-text search (`q`).
- Sorting, and pagination driven by `X-Total-Count`.
- Filter, sort and page state kept in the URL search params.
- The summary strip.
- A Refresh button.

## Acceptance criteria
- [ ] Filtering by Critical + Open, sorting by created date, and paging all work. Reloading the URL reproduces the same view.
- [ ] The summary counts match the seed data.

Refs: spec § 2

## Comments

- Implemented on branch worktree-agent-a672c1bf8e15d2851; verified against the running server.
