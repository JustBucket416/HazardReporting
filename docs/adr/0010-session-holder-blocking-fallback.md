# SessionHolder hides a single runBlocking fallback

OkHttp interceptors are synchronous, but the session token lives in DataStore, which can only be read with suspend functions. Making every Retrofit method wait for the session doesn't scale as endpoints are added.

So the `@Singleton SessionHolder` keeps the token in memory, as a `Loaded(session)` state rather than a nullable token. Its getter returns the in-memory value, and **only if the state hasn't been loaded yet** does it fall back to `runBlocking { dataStore.read() }`.

Loading starts as early as possible. `Application.onCreate` builds the Dagger graph and then launches a coroutine in the application scope that loads the session, so the fallback almost never runs. `AuthInterceptor` is the only caller of the blocking getter, and it runs on OkHttp's background threads, never on Main. Login and logout update the in-memory state and DataStore together.

## Considered Options

- **`runBlocking` in `Application.onCreate`**: rejected, because it blocks app startup on the main thread.
- **Every network call does `waitForToken()` (join on the load job)**: works, but every new endpoint must remember to call it.

## Consequences

- The in-memory state must tell "not loaded yet" apart from "loaded, logged out". If it were a plain nullable token, every request made while logged out (for example `POST /login`) would block on a DataStore read.
