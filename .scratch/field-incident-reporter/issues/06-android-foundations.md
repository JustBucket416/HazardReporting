# Android: foundations

Status: ready-for-agent
Type: task
Blocked by: —

## What
Prepare the template for the rest of the app (spec § 3.1):
- Set `minSdk 26`.
- Add every library to `libs.versions.toml`, at the versions current on the day this is implemented. The project uses **AGP 9.x with built-in Kotlin**, so check how KSP is wired against AGP 9 rather than copying CurrencyConverter's plugin block.
- Create the packages `domain`, `data`, `presentation`, `di` and `utils`.
- Add `utils/CoroutineDispatchers` (io, default, main), and `utils/Logger` with an `AndroidLogger` implementation.
- Add a `HazardReportingApp` Application and a Dagger `AppComponent` (`@Singleton`) with:
  - an `@ApplicationScope CoroutineScope`
  - a multibound `ViewModelFactory` with a `ViewModelKey`
  - empty modules
- Theme:
  - fixed brand light and dark schemes
  - severity and status colour tokens
  - edge-to-edge
- Add `presentation/common/Modifier.shimmer()`.
- Test setup: `MainCoroutineRule`, MockK, Truth, Turbine and coroutines-test.
- Debug `network_security_config` that allows cleartext to `10.0.2.2` and `localhost`.

## Acceptance criteria
- [ ] `./gradlew assembleDebug testDebugUnitTest` passes.
- [ ] The app launches into an empty themed screen, with the Dagger graph built in `HazardReportingApp`.
- [ ] A shimmer preview renders in both light and dark.

Refs: ADR-0008, ADR-0009

## Comments

- Implemented on feat/android-app in 1e47342; unit tests green.
