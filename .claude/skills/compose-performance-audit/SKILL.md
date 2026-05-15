---
name: compose-performance-audit
description: "Аудит производительности Jetpack Compose: recomposition, unstable keys, lazy lists, profiling. Триггеры: performance, recomposition, jank, slow rendering, lazy column performance, stability."
---

# Compose Performance Audit

## Overview

Audit Jetpack Compose view performance end-to-end, from instrumentation and baselining to root-cause analysis and concrete remediation steps.

## Workflow Decision Tree

- If the user provides code, start with "Code-First Review."
- If the user only describes symptoms, ask for minimal code/context, then do "Code-First Review."
- If code review is inconclusive, go to "Guide the User to Profile."

## 1. Code-First Review

Focus on:
- **Recomposition storms** from unstable parameters or broad state changes.
- **Unstable keys** in `LazyColumn`/`LazyRow` (`key` churn, missing keys).
- **Heavy work in composition** (formatting, sorting, filtering, object allocation).
- **Unnecessary recompositions** (missing `remember`, unstable classes, lambdas).
- **Large images** without proper sizing or async loading.
- **Layout thrash** (deep nesting, intrinsic measurements, `SubcomposeLayout` misuse).

## 2. Guide the User to Profile

- Use **Layout Inspector** in Android Studio to see recomposition counts.
- Enable **Recomposition Highlights** in Compose tooling.
- Use **Perfetto** or **System Trace** for frame timing analysis.
- Check **Macrobenchmark** results for startup/scroll metrics.

> **Important**: Ensure profiling is done on a **release build** with R8 enabled.

## 3. Remediate

- **Stabilize parameters**: Use `@Stable` or `@Immutable` annotations on data classes.
- **Stabilize keys**: Use stable, unique IDs for `LazyColumn`/`LazyRow` items.
- **Defer state reads**: Use `derivedStateOf`, lambda-based modifiers, or `Modifier.drawBehind`.
- **Remember expensive computations**: Wrap in `remember { }` or `remember(key) { }`.
- **Skip recomposition**: Extract stable composables, use `key()` to control identity.
- **Async image loading**: Use Coil/Glide with proper sizing constraints.
- **Reduce layout complexity**: Flatten hierarchies, avoid deep nesting.

## Common Code Smells (and Fixes)

### Unstable lambda captures

```kotlin
// BAD: New lambda instance every recomposition
Button(onClick = { viewModel.doSomething(item) }) { ... }

// GOOD: Use remember or method reference
val onClick = remember(item) { { viewModel.doSomething(item) } }
Button(onClick = onClick) { ... }
```

### Expensive work in composition

```kotlin
// BAD: Sorting on every recomposition
@Composable
fun ItemList(items: List<Item>) {
    val sorted = items.sortedBy { it.name }
    LazyColumn { items(sorted) { ... } }
}

// GOOD: Use remember with key
@Composable
fun ItemList(items: List<Item>) {
    val sorted = remember(items) { items.sortedBy { it.name } }
    LazyColumn { items(sorted) { ... } }
}
```

### Missing keys in LazyColumn

```kotlin
// BAD: Index-based identity
LazyColumn { items(items) { item -> ItemRow(item) } }

// GOOD: Stable key-based identity
LazyColumn { items(items, key = { it.id }) { item -> ItemRow(item) } }
```

### Unstable data classes

```kotlin
// BAD: Unstable (contains List)
data class UiState(val items: List<Item>, val isLoading: Boolean)

// GOOD: Mark as Immutable
@Immutable
data class UiState(val items: ImmutableList<Item>, val isLoading: Boolean)
```

### Reading state too early

```kotlin
// BAD: State read during composition
@Composable
fun AnimatedBox(scrollState: ScrollState) {
    val offset = scrollState.value
    Box(modifier = Modifier.offset(y = offset.dp)) { ... }
}

// GOOD: Defer state read to layout phase
@Composable
fun AnimatedBox(scrollState: ScrollState) {
    Box(modifier = Modifier.offset {
        IntOffset(0, scrollState.value)
    }) { ... }
}
```

## Stability Checklist

| Type | Stable by Default? | Fix |
|------|-------------------|-----|
| Primitives (`Int`, `String`, `Boolean`) | Yes | N/A |
| `data class` with stable fields | Yes* | Ensure all fields are stable |
| `List`, `Map`, `Set` | **No** | Use `ImmutableList` from kotlinx |
| Classes with `var` properties | **No** | Use `@Stable` if externally stable |
| Lambdas | **No** | Use `remember { }` |

## References

- [Jetpack Compose Performance](https://developer.android.com/develop/ui/compose/performance)
- [Compose Stability Explained](https://developer.android.com/develop/ui/compose/performance/stability)
