# Duplicate creates are detected by reporter + createdAt

A POST can succeed on the server while its response never reaches the phone, for example after a timeout or a dropped connection. The client must then retry, and the server has to recognise that retry. The server generates incident ids, so the client has none to send.

We use a natural key: the reporter (taken from the token) plus `createdAt`. `createdAt` is set on the device, in epoch milliseconds, when the technician first saves the report, and it never changes. If an incident with that key already exists, the server updates it and returns 200, exactly as it does for a first-time create.

## Considered Options

- **Match on id + title**: rejected. The client has no id on a create, and titles repeat. Two genuine incidents titled "Broken light" would merge into one, silently losing a report.
- **A client idempotency key (UUID)**: robust, but it adds a field whose only purpose is transport. Since `createdAt` is already immutable and practically unique per reporter, it does the same job.

## Consequences

- `createdAt` must be sent and compared as an integer. An ISO string could differ in formatting or precision and break the exact match.
- Withdrawing an incident whose create may have reached the server goes through POST-then-PATCH, so no Open orphan is left behind. The row is deleted locally only if no POST was ever attempted.
