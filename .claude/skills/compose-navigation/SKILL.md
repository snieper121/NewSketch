---
name: compose-navigation
description: "Navigation Compose: NavHost, type-safe routes, arguments, deep links, nested graphs. Триггеры: navigation, NavHost, navigate, route, deep link, bottom navigation, back stack."
---

# Compose Navigation

## Overview

Implement type-safe navigation in Jetpack Compose applications using the Navigation Compose library.

## Setup

```kotlin
dependencies {
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
}
plugins {
    kotlin("plugin.serialization") version "2.0.21"
}
```

---

## Core Concepts

### 1. Define Routes (Type-Safe)

```kotlin
import kotlinx.serialization.Serializable

@Serializable object Home
@Serializable data class Profile(val userId: String)
@Serializable data class Settings(val section: String? = null)
@Serializable data class ProductDetail(val productId: String, val showReviews: Boolean = false)
```

### 2. Create NavController

```kotlin
@Composable
fun MyApp() {
    val navController = rememberNavController()
    AppNavHost(navController = navController)
}
```

### 3. Create NavHost

```kotlin
@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Home,
        modifier = modifier
    ) {
        composable<Home> {
            HomeScreen(
                onNavigateToProfile = { userId -> navController.navigate(Profile(userId)) }
            )
        }
        composable<Profile> { backStackEntry ->
            val profile: Profile = backStackEntry.toRoute()
            ProfileScreen(userId = profile.userId)
        }
    }
}
```

---

## Navigation Patterns

### Basic Navigation

```kotlin
navController.navigate(Profile(userId = "user123"))
navController.navigate(Home) { popUpTo<Home> { inclusive = true } }
navController.popBackStack()
```

### Bottom Navigation Pattern

```kotlin
navController.navigate(Home) {
    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}
```

---

## Deep Links

```kotlin
composable<Profile>(
    deepLinks = listOf(navDeepLink<Profile>(basePath = "https://example.com/profile"))
) { backStackEntry ->
    val profile: Profile = backStackEntry.toRoute()
    ProfileScreen(userId = profile.userId)
}
```

---

## Nested Navigation

```kotlin
NavHost(navController = navController, startDestination = Home) {
    composable<Home> { HomeScreen() }
    navigation<AuthGraph>(startDestination = Login) {
        composable<Login> { LoginScreen() }
        composable<Register> { RegisterScreen() }
    }
}

@Serializable object AuthGraph
@Serializable object Login
@Serializable object Register
```

---

## Critical Rules

### DO
- Use `@Serializable` routes for type safety
- Pass only IDs/primitives as arguments
- Use `popUpTo` with `launchSingleTop` for bottom navigation
- Extract `NavHost` to a separate composable for testability

### DON'T
- Pass complex objects as navigation arguments
- Create `NavController` inside `NavHost`
- Use string-based routes (legacy pattern)
