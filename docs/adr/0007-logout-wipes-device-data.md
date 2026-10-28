# Logout wipes all data on the device, after trying to send pending changes

Only one technician's data exists on a device at any time, so there is no per-user partitioning in Room. Logout first tries to push pending changes. If any remain unsent, the user must confirm "N reports will be lost" before logging out. Then the wipe runs in this order:
1. Cancel WorkManager jobs and wait for the sync `Mutex`.
2. Clear every Room table, including the catalog.
3. Delete the private photo files.
4. Clear Coil's caches.
5. Clear DataStore.
6. Best-effort `POST /logout`.

The app never logs out on its own. A 401 only stops sync and shows a banner.

## Considered Options

- **Keep pending changes per user until that user logs back in**: rejected. It is more complex, and it leaves one technician's data on a shared device.
