# Strict Clean Architecture in one module, with concrete interactors grouped by model

The Android app is a single `:app` module with four packages: `domain`, `data`, `presentation` and `di`. Dependencies point inward: presentation → domain ← data. `di` knows all three and lives on its own.

- **`domain`** is pure Kotlin. It holds the models, the repository and `LocationProvider` interfaces, the domain exceptions, and the interactors.
- **Interactors replace use-case classes.** There is one per model (`IncidentInteractor`, `LoginInteractor`, `CatalogInteractor`), and each method is one use case.
- **Interactors are concrete classes** with no interface. Dagger provides them with `@Provides`, and tests mock them with MockK.
- **ViewModels depend only on interactors**, never on repositories.
- **In `data`**, each repository implementation composes concrete `XxxLocalDataSource` and `XxxRemoteDataSource` classes, plus injected converter classes. The push algorithm stays in `data/sync`, because it is persistence and transport policy.

## Considered Options

- **The reference project's style** (CurrencyConverter): interactors call data sources directly, and converters live in `domain`, so `domain` depends on `data`. Rejected for this project in favour of strict dependency inversion.
- **Interfaces for interactors and data sources**: they add nothing, because MockK can mock concrete classes.
