---
name: xml-to-compose-migration
description: "Миграция XML layouts на Jetpack Compose: layout mapping, state migration, incremental adoption. Триггеры: XML to Compose, migrate views, convert layout, ConstraintLayout, RecyclerView, ViewBinding."
---

# XML to Compose Migration

## Overview

Systematically convert Android XML layouts to idiomatic Jetpack Compose, preserving functionality while embracing Compose patterns.

## Workflow

### 1. Analyze the XML Layout
- Identify the root layout type (`ConstraintLayout`, `LinearLayout`, `FrameLayout`, etc.).
- List all View widgets and their key attributes.
- Map data binding expressions (`@{}`) or view binding references.
- Identify custom views that need special handling.

### 2. Plan the Migration
- Decide: **Full rewrite** or **incremental migration** (using `ComposeView`/`AndroidView`).
- Identify state sources (ViewModel, LiveData, savedInstanceState).
- List reusable components to extract as separate Composables.

### 3. Convert Layouts

#### Container Layouts

| XML Layout | Compose Equivalent | Notes |
|------------|-------------------|-------|
| `LinearLayout (vertical)` | `Column` | Use `Arrangement` and `Alignment` |
| `LinearLayout (horizontal)` | `Row` | Use `Arrangement` and `Alignment` |
| `FrameLayout` | `Box` | Children stack on top of each other |
| `ConstraintLayout` | `ConstraintLayout` (Compose) | Use `createRefs()` and `constrainAs` |
| `ScrollView` | `Column` + `Modifier.verticalScroll()` | Or use `LazyColumn` for lists |
| `RecyclerView` | `LazyColumn` / `LazyRow` / `LazyGrid` | Most common migration |
| `ViewPager2` | `HorizontalPager` | From Compose Foundation |
| `CoordinatorLayout` | Custom + `Scaffold` | Use `TopAppBar` with scroll behavior |

#### Common Widgets

| XML Widget | Compose Equivalent | Notes |
|------------|-------------------|-------|
| `TextView` | `Text` | Use `style` → `TextStyle` |
| `EditText` | `TextField` / `OutlinedTextField` | Requires state hoisting |
| `Button` | `Button` | Use `onClick` lambda |
| `ImageView` | `Image` | Use `painterResource()` or Coil |
| `CheckBox` | `Checkbox` | Requires `checked` + `onCheckedChange` |
| `Switch` | `Switch` | Requires state hoisting |
| `ProgressBar` | `CircularProgressIndicator` / `LinearProgressIndicator` | |
| `CardView` | `Card` | From Material 3 |
| `Toolbar` | `TopAppBar` | Use inside `Scaffold` |
| `BottomNavigationView` | `NavigationBar` | Material 3 |
| `FloatingActionButton` | `FloatingActionButton` | Use inside `Scaffold` |

#### Attribute Mapping

| XML Attribute | Compose Modifier/Property |
|---------------|--------------------------|
| `android:layout_width="match_parent"` | `Modifier.fillMaxWidth()` |
| `android:layout_height="match_parent"` | `Modifier.fillMaxHeight()` |
| `android:layout_weight` | `Modifier.weight(1f)` |
| `android:padding` | `Modifier.padding()` |
| `android:layout_margin` | `Modifier.padding()` on parent |
| `android:background` | `Modifier.background()` |
| `android:visibility="gone"` | Conditional composition (don't emit) |
| `android:clickable` | `Modifier.clickable { }` |
| `android:contentDescription` | `Modifier.semantics { contentDescription = "" }` |
| `android:elevation` | `Modifier.shadow()` or component's `elevation` param |
| `android:alpha` | `Modifier.alpha()` |
| `android:gravity` | `Alignment` parameter or `Arrangement` |

### 4. Migrate State

```kotlin
// Before: Observing in Fragment
viewModel.uiState.observe(viewLifecycleOwner) { state ->
    binding.title.text = state.title
}

// After: Collecting in Compose
@Composable
fun MyScreen(viewModel: MyViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Text(text = uiState.title)
}
```

### 5. Incremental Migration (Interop)

```xml
<!-- Embedding Compose in XML -->
<androidx.compose.ui.platform.ComposeView
    android:id="@+id/compose_view"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />
```

```kotlin
// Embedding XML Views in Compose
@Composable
fun MapViewComposable(modifier: Modifier = Modifier) {
    AndroidView(
        factory = { context -> MapView(context).apply { /* Initialize */ } },
        update = { mapView -> /* Update on state changes */ },
        modifier = modifier
    )
}
```

## Checklist

- [ ] All layouts converted (no `include` or `merge` left)
- [ ] State hoisted properly
- [ ] Click handlers converted to lambdas
- [ ] RecyclerView adapters removed (using LazyColumn/LazyRow)
- [ ] ViewBinding/DataBinding removed
- [ ] Navigation integrated
- [ ] Theming applied (MaterialTheme)
- [ ] Accessibility preserved
- [ ] Preview annotations added
- [ ] Old XML files deleted
