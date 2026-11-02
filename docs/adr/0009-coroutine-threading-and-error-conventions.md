# The producer picks the thread; each layer catches only its own exceptions

**Threading.** The code that produces data decides which thread it runs on:
- Repositories switch with `withContext(dispatchers.io)` and `flowOn(dispatchers.io)`.
- Interactors never switch context.
- ViewModels launch with `viewModelScope.launch(exceptionHandler)`, on Main. Interactor work runs there too, which is safe because every blocking call sits behind a repository that switches to IO.
- UI state is still changed only through `MutableStateFlow.update {}`.

Dispatchers come from an injected `CoroutineDispatchers` class, which tests mock.

**Errors.** Each layer catches only the exception types it owns:
- The data layer translates transport failures into domain exceptions in one OkHttp interceptor: no connection, a rejected session and wrong credentials. They extend `IOException`, as OkHttp requires of anything thrown from an interceptor. Other HTTP errors stay `HttpException`s for the data layer to handle, and everything else goes through.
- The domain layer throws `ValidationException`.
- ViewModels catch the domain exceptions they can show to the user.
- Anything else reaches the shared `CoroutineExceptionHandler`. It logs through the `Logger` object (mocked with `mockkObject` in JVM tests) and calls the ViewModel's `onUnexpected` to clear loading state.

`runCatching` is banned in coroutine code because it also swallows `CancellationException`.

## Considered Options

- **The ViewModel switches to IO and back to Main**, as in the reference project. Rejected: the data producer should own the threading choice.
- **ViewModels launching on the Default dispatcher**, so that interactor work never runs on Main. Used at first, then dropped: UI state reached the screen after a thread hop, and the report form needed a second state flow so its text fields wouldn't drop keystrokes.
