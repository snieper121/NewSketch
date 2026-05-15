---
name: gradle-build-performance
description: "Оптимизация сборки Gradle: configuration cache, build cache, parallel, KSP vs kapt, CI/CD. Триггеры: build slow, gradle performance, build scan, configuration cache, kapt, KSP, CI build."
---

# Gradle Build Performance

## When to Use

- Build times are slow (clean or incremental)
- Investigating build performance regressions
- Analyzing Gradle Build Scans
- Identifying configuration vs execution bottlenecks
- Optimizing CI/CD build times
- Enabling Gradle Configuration Cache
- Reducing unnecessary recompilation
- Debugging kapt/KSP annotation processing

## Workflow

1. **Measure Baseline** — Clean build + incremental build times
2. **Generate Build Scan** — `./gradlew assembleDebug --scan`
3. **Identify Phase** — Configuration? Execution? Dependency resolution?
4. **Apply ONE optimization** — Don't batch changes
5. **Measure Improvement** — Compare against baseline
6. **Verify in Build Scan** — Visual confirmation

---

## Quick Diagnostics

```bash
./gradlew assembleDebug --scan          # Build scan
./gradlew assembleDebug --profile       # Local profile report
```

---

## 12 Optimization Patterns

### 1. Enable Configuration Cache
```properties
org.gradle.configuration-cache=true
org.gradle.configuration-cache.problems=warn
```

### 2. Enable Build Cache
```properties
org.gradle.caching=true
```

### 3. Enable Parallel Execution
```properties
org.gradle.parallel=true
```

### 4. Increase JVM Heap
```properties
org.gradle.jvmargs=-Xmx4g -XX:+UseParallelGC
```

### 5. Use Non-Transitive R Classes
```properties
android.nonTransitiveRClass=true
```

### 6. Migrate kapt to KSP
```kotlin
// Before (slow)
kapt("com.google.dagger:hilt-compiler:2.51.1")
// After (fast)
ksp("com.google.dagger:hilt-compiler:2.51.1")
```

### 7. Avoid Dynamic Dependencies
```kotlin
// BAD: Forces resolution every build
implementation("com.example:lib:+")
// GOOD: Fixed version
implementation("com.example:lib:1.2.3")
```

### 8. Optimize Repository Order
```kotlin
dependencyResolutionManagement {
    repositories {
        google()      // First: Android dependencies
        mavenCentral() // Second: Most libraries
    }
}
```

### 9. Use includeBuild for Local Modules
```kotlin
includeBuild("shared-library") {
    dependencySubstitution {
        substitute(module("com.example:shared")).using(project(":"))
    }
}
```

### 10. Enable Incremental Annotation Processing
```properties
kapt.incremental.apt=true
kapt.use.worker.api=true
```

### 11. Avoid Configuration-Time I/O
```kotlin
// BAD: Runs during configuration
val version = file("version.txt").readText()
// GOOD: Defer to execution
val version = providers.fileContents(file("version.txt")).asText
```

### 12. Use Lazy Task Configuration
```kotlin
// BAD: Eagerly configured
tasks.create("myTask") { ... }
// GOOD: Lazily configured
tasks.register("myTask") { ... }
```

---

## CI/CD Optimizations

### Remote Build Cache
```kotlin
buildCache {
    local { isEnabled = true }
    remote<HttpBuildCache> {
        url = uri("https://cache.example.com/")
        isPush = System.getenv("CI") == "true"
    }
}
```

### Skip Unnecessary Tasks in CI
```bash
./gradlew assembleDebug -x test -x lint
./gradlew :feature:login:test
```

---

## Verification Checklist

- [ ] Configuration cache enabled and working
- [ ] Build cache hit rate > 80%
- [ ] No dynamic dependency versions
- [ ] KSP used instead of kapt where possible
- [ ] Parallel execution enabled
- [ ] JVM memory tuned appropriately
- [ ] CI remote cache configured
- [ ] No configuration-time I/O
