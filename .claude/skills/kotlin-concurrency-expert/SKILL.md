---
name: kotlin-concurrency-expert
description: "Kotlin Coroutines ревью: structured concurrency, lifecycle safety, scoping, exception handling. Триггеры: coroutine review, concurrency bug, memory leak, ANR, race condition, CancellationException, GlobalScope."
---

# Kotlin Concurrency Expert

## Overview

Review and fix Kotlin Coroutines issues in Android codebases by applying structured concurrency, lifecycle safety, proper scoping, and modern best practices with minimal behavior changes.

## Workflow

### 1. Triage the Issue

- Capture the exact error, crash, or symptom (ANR, memory leak, race condition, incorrect state).
- Check project coroutines setup: `kotlinx-coroutines-android` version, `lifecycle-runtime-ktx` version.
- Identify the current scope context (`viewModelScope`, `lifecycleScope`, custom scope, or none).
- Confirm whether the code is UI-bound (`Dispatchers.Main`) or intended to run off the main thread.
- Verify Dispatcher injection patterns for testability.

### 2. Apply the Smallest Safe Fix

Common fixes:

- **ANR / Main thread blocking**: Move heavy work to `withContext(Dispatchers.IO)` or `Dispatchers.Default`.
-   **Memory leaks / zombie coroutines**: Replace `GlobalScope` with a lifecycle-bound scope.
-   **Lifecycle collection issues**: Replace deprecated `launchWhenStarted` with `repeatOnLifecycle(Lifecycle.State.STARTED)`.
-   **State exposure**: Encapsulate `MutableStateFlow` / `MutableSharedFlow`; expose read-only.
-   **CancellationException swallowing**: Ensure generic `catch (e: Exception)` blocks rethrow `CancellationException`.
-   **Non-cooperative cancellation**: Add `ensureActive()` or `yield()` in tight loops.
-   **Callback APIs**: Convert listeners to `callbackFlow` with proper `awaitClose` cleanup.
-   **Hardcoded Dispatchers**: Inject `CoroutineDispatcher` via constructor for testability.

## Critical Rules

### Dispatcher Injection (Testability)

```kotlin
// CORRECT
class UserRepository(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend fun fetchUser() = withContext(ioDispatcher) { ... }
}
```

### Lifecycle-Aware Collection

```kotlin
// CORRECT
viewLifecycleOwner.lifecycleScope.launch {
    viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.uiState.collect { state -> updateUI(state) }
    }
}
```

### State Encapsulation

```kotlin
// CORRECT
class MyViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
}
```

### Exception Handling

```kotlin
// CORRECT: Rethrow CancellationException
try {
    doSuspendWork()
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    handleError(e)
}
```

### Cooperative Cancellation

```kotlin
suspend fun processLargeList(items: List<Item>) {
    items.forEach { item ->
        ensureActive() // Check cancellation
        processItem(item)
    }
}
```

### Callback Conversion

```kotlin
fun locationUpdates(): Flow<Location> = callbackFlow {
    val listener = LocationListener { location -> trySend(location) }
    locationManager.requestLocationUpdates(listener)
    awaitClose { locationManager.removeUpdates(listener) }
}
```

## Scope Guidelines

| Scope | Use When | Lifecycle |
|-------|----------|-----------|
| `viewModelScope` | ViewModel operations | Cleared with ViewModel |
| `lifecycleScope` | UI operations in Activity/Fragment | Destroyed with lifecycle owner |
| `repeatOnLifecycle` | Flow collection in UI | Started/Stopped with lifecycle state |
| `applicationScope` (injected) | App-wide background work | Application lifetime |
| `GlobalScope` | **NEVER USE** | Breaks structured concurrency |

## Testing Pattern

```kotlin
@Test
fun `loading data updates state`() = runTest {
    val testDispatcher = StandardTestDispatcher(testScheduler)
    val repository = FakeRepository()
    val viewModel = MyViewModel(repository, testDispatcher)
    viewModel.loadData()
    advanceUntilIdle()
    assertEquals(UiState.Success(data), viewModel.uiState.value)
}
```
