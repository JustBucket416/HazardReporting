# The list holds summaries; details load on demand and are cached until refreshed

The technician app does not mirror full incidents. After login, and on list pull-to-refresh, it downloads only summaries from `GET /incidents/summary`: id, title, category, severity, status, createdAt and updatedAt. An incident's details (description, site, location, photos, priority) are fetched the first time its detail screen opens.

After that, the details are reused until the user pulls to refresh on that detail screen. The repository decides when to fetch: `force || details == null`.

We deliberately **accept staleness**: we don't compare `updatedAt` and we don't use a TTL. Cached details may show an old priority, or offer Edit/Withdraw on an incident an operator has since acknowledged. The 409 path from ADR-0004 corrects that the moment the user acts.

## Consequences

- An incident whose details were never downloaded shows its summary, with a shimmer in place of the details. Offline, that becomes "Details not downloaded / Retry". The Edit/Withdraw menu appears only once details are loaded.
