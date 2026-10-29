# The producer picks the thread; each layer catches only its own exceptions

**Threading.** The code that produces data decides which thread it runs on:
- Repositories switch with `withContext(dispatchers.io)` and `flowOn(dispatchers.io)`.
- Interactors never switch context.
- ViewModels launch with `viewModelScope.launch(exceptionHandler + dispatchers.default)`, so interactor work runs off Main.
- Because ViewModel code runs on the multi-threaded Default dispatcher, UI state is changed only through `MutableStateFlow.update {}`.

Dispatchers come from an injected `CoroutineDispatchers` class, which tests mock.

**Errors.** Each layer catches only the exception types it owns:
- The data layer translates transport failures into domain exceptions in one OkHttp interceptor: no connection, a rejected session and wrong credentials. They extend `IOException`, as OkHttp requires of anything thrown from an interceptor. Other HTTP errors stay `HttpException`s for the data layer to handle, and everything else goes through.
- The domain layer throws `ValidationException`.
- ViewModels catch the domain exceptions they can show to the user.
- Anything else reaches the shared `CoroutineExceptionHandler`. It logs through the `Logger` object (mocked with `mockkObject` in JVM tests) and calls the ViewModel's `onUnexpected` to clear loading state.

`runCatching` is banned in coroutine code because it also swallows `CancellationException`.

## Considered Options

- **The ViewModel switches to IO and back to Main**, as in the reference project. Rejected: the data producer should own the threading choice.
- **Main-safe data with ViewModels launching on Main.** Rejected so that interactor work never runs on the UI thread.
