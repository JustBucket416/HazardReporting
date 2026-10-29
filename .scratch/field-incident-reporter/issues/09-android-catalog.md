# Android: catalog (categories, severities, sites)

Status: ready-for-agent
Type: task
Blocked by: 07

## What
- Room tables and DAOs for categories, severities and sites.
- `CatalogLocalDataSource` and `CatalogRemoteDataSource`.
- `CatalogRepository`: the interface in domain and the implementation in data, with `refresh()` and the `observe*()` flows.
- `CatalogInteractor`.
- Converters.
- Dagger `DatabaseModule` and `RepositoryModule` bindings.

## Acceptance criteria
- [ ] After `refresh()`, the observed flows emit the seeded catalog, including when offline.
- [ ] Converter and repository unit tests pass.

Refs: spec § 3.2–3.3

## Comments

- Implemented on feat/android-app in 6a3ddcf; unit tests green.
