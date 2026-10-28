# Android: network stack and session

Status: ready-for-agent
Type: task
Blocked by: 06

## What
- `NetworkModule`:
  - OkHttp, with the logging interceptor in debug builds only
  - Retrofit with the kotlinx.serialization converter and `ignoreUnknownKeys`
  - the `BASE_URL` constant next to the Retrofit builder
- Remote error mapping:
  - `IOException` → `NoConnectionException`
  - 401 → `UnauthorizedException`, or `InvalidCredentialsException` for `/login`
- Session data sources:
  - `SessionLocalDataSource` on DataStore
  - `SessionRemoteDataSource` for `/login` and `/logout`
- `SessionHolder`:
  - its state is `Loading | Loaded(session?)`
  - the blocking getter calls `runBlocking` only while the state is `Loading`
  - `Application.onCreate` launches the load in the app scope right after the Dagger graph is built
- `AuthInterceptor`: reads the token from `SessionHolder`; skips `/login`.
- `SessionRepository`: the interface in domain, the implementation in data.

## Acceptance criteria
- [ ] Unit tests cover the `SessionHolder` states:
  - loaded with a session
  - loaded and logged out, which must not block
  - a getter call before the load finishes, which falls back to the blocking read
- [ ] Error-mapping unit tests cover: an IO failure, a 401 on `/login`, a 401 elsewhere, and a 500 that propagates.
- [ ] `AuthInterceptor` adds the header only when there is a token and the request isn't to `/login`.

Refs: ADR-0010, ADR-0009
