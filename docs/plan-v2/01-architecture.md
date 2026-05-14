# 01 — Архитектура Ai IDE

## Стек IDE-приложения

| Слой | Технология | Версия |
|------|-----------|--------|
| UI | Jetpack Compose + Material3 | BOM 2024.12.01 |
| Presentation | ViewModel + StateFlow (MVI) | lifecycle 2.8.7 |
| DI | AppContainer (MVP) → Koin (M3+) | koin-android 4.0+ |
| Navigation | Navigation Compose | 2.8.5 |
| Storage | Room (метаданные) + FileSystem (исходники) | room 2.6.1 |
| Network | Ktor Client | 3.0.2 |
| Serialization | Kotlinx Serialization | 1.7.3 |
| Code Editor | Sora Editor (M2+) / OutlinedTextField (M0-M1) | sora 0.23 |
| Logging | Timber | 5.0.1 |
| Codegen | KotlinPoet | 1.18.1 |
| Security | EncryptedSharedPreferences (API keys) | security-crypto 1.1.0-alpha |
| Crash reporting | opt-in, Firebase Crashlytics (v0.2+) | — |

## Модульная структура: эволюция

**Решение:** начинаем с **single-module `:app`**, дробим в M3+.

### M0–M2: single module (текущее состояние)

```
app/
├── AiApp.kt
├── MainActivity.kt
├── build/       ← сборочный pipeline (новый пакет)
├── codegen/     ← JSON → Kotlin (новый пакет)
├── data/
│   ├── model/
│   ├── local/
│   ├── remote/
│   ├── repository/
│   └── template/
├── di/          ← AppContainer
└── ui/
    ├── navigation/
    ├── screens/
    └── theme/
```

**Почему single-module сейчас:**
- Быстрый итерационный цикл (нет overhead на multi-module configuration)
- Проще отлаживать
- Android Studio friendly (не все парсеры Gradle любят 10+ модулей)
- Нет премьерной необходимости изоляции

### M3+: multi-module split

Когда граф зависимостей усложнится:

```
:app              — Activity, DI setup, navigation graph
:core:model       — domain models (Project, Screen, Widget...)
:core:data        — repositories, Room DB, file operations
:core:common      — extensions, utils, Result, UiEvent
:feature:home     — project list, create project
:feature:editor   — visual editor, code editor, property panel
:feature:build    — build pipeline UI, logs
:feature:ai       — AI chat, diff viewer
:feature:settings — app settings, project settings
:lib:codegen      — KotlinPoet-based code generation
:lib:compiler     — in-process kotlinc/ecj/d8/aapt2 invocation
:lib:toolchain    — download, verify, manage build tools
```

**Триггер разбивки:** когда `:app` содержит > 100 файлов ИЛИ команда > 2 человек.

## Архитектурные слои

```
┌───────────────────────────────────────┐
│           UI Layer (Compose)          │
│  Screens, Components, Theme          │
├───────────────────────────────────────┤
│         Presentation Layer            │
│  ViewModels, UiState, UiEvents (MVI) │
├───────────────────────────────────────┤
│           Domain Layer                │
│  UseCases (M2+), Domain Models       │
├───────────────────────────────────────┤
│            Data Layer                 │
│  Room, FileSystem, Ktor, Prefs       │
├───────────────────────────────────────┤
│           Build Layer                 │
│  ToolchainManager, Compiler, Signer  │
└───────────────────────────────────────┘
```

**Note:** Domain Layer (UseCases) появляется в M2+. В M0-M1 ViewModels напрямую используют Repositories.

## Dependency Injection

### MVP (M0-M2): AppContainer

Ручной контейнер зависимостей. Достаточно на этом этапе:
- Все тяжёлые объекты — lazy
- Контейнер живёт столько же, сколько процесс
- Нет overhead от аннотаций/KSP

### M3+: Koin

Когда граф вырастет (10+ services, scoped dependencies, multi-module):

```kotlin
val dataModule = module {
    single { AppDatabase.create(androidContext()) }
    single<ProjectDao> { get<AppDatabase>().projectDao() }
    single { ProjectStorage(androidContext()) }
    single { HttpClientFactory.create() }
}

val repositoryModule = module {
    single { SettingsRepository(androidContext()) }
    single { ProjectRepository(get(), get(), get()) }
    single<AiService> { OpenAiCompatibleService(get(), get()) }
    single { AiRepository(get()) }
}

val viewModelModule = module {
    viewModel { ProjectsViewModel(get()) }
    viewModel { EditorViewModel(get(), get()) }
    viewModel { ChatViewModel(get(), get()) }
    viewModel { SettingsViewModel(get()) }
}
```

**Почему Koin, не Hilt:**
- Runtime DI, нет KSP (быстрее сборка)
- Меньше boilerplate
- Лучше работает с multi-module без jar hell
- Для IDE-приложения скорость DI — не bottleneck

## Storage Model (Android 10–16)

### Принцип: app-private + SAF

Все данные IDE хранятся в `context.filesDir` — работает на API 29–36 без permissions.

```
context.filesDir/
├── projects/
│   └── <uuid>/
│       ├── design/          ← JSON source of truth
│       ├── generated/       ← auto-generated .kt (read-only)
│       ├── custom/          ← user Kotlin code (preserved)
│       └── build/           ← compilation output
├── keystore/                ← debug keystore
context.noBackupFilesDir/
├── toolchain/               ← kotlinc, ecj, aapt2, d8... (lazy-downloaded)
├── cache/                   ← AAR/JAR cache
└── templates/               ← project templates
```

**`noBackupFilesDir`** для toolchain (~136 MB) — не попадает в Google Auto Backup (лимит 25 MB).

### Импорт/Экспорт (SAF)

```kotlin
// Export: пользователь выбирает папку через системный picker
val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)

// Import: пользователь выбирает файл
val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
```

Никаких `MANAGE_EXTERNAL_STORAGE`, `READ_EXTERNAL_STORAGE` — не нужны.

## API-Level Compatibility Matrix

| API | Что учитываем в IDE |
|-----|---------------------|
| 29 | minSdk. Scoped storage — только app-private |
| 30 | Полный enforcement scoped storage. `<queries>` в Manifest для запуска собранных APK |
| 31 | `android:exported` обязателен. Splash Screen API |
| 33 | `POST_NOTIFICATIONS` permission для уведомления о завершении сборки |
| 34 | W^X — только in-process class loading. Foreground service type обязателен |
| 35 | Edge-to-edge enforced. 16-KB page alignment для native (aapt2, zipalign) |
| 36 | targetSdk. Мониторим beta на предмет DexClassLoader ограничений |

## Manifest (ключевые элементы)

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />

<queries>
    <intent>
        <action android:name="android.intent.action.VIEW" />
        <data android:scheme="content" />
    </intent>
    <intent>
        <action android:name="android.intent.action.INSTALL_PACKAGE" />
    </intent>
</queries>

<service
    android:name=".build.BuildService"
    android:foregroundServiceType="dataSync"
    android:exported="false" />

<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>
```

## Generated Apps — структура

```
generated/
├── AndroidManifest.xml
├── src/main/kotlin/com/example/app/
│   ├── MainActivity.kt
│   ├── App.kt              ← NavHost
│   ├── theme/
│   │   ├── Theme.kt
│   │   ├── Color.kt
│   │   └── Type.kt
│   └── screens/
│       ├── MainScreen.kt
│       └── SettingsScreen.kt
└── src/main/res/
    ├── values/strings.xml
    ├── values/themes.xml   ← launcher splash theme
    └── mipmap/             ← launcher icons
```

## Error Handling (единый pattern)

### Domain Layer: Result<T>

```kotlin
sealed class Result<out T> {
    data class Success<T>(val value: T) : Result<T>()
    data class Error(val cause: AppError) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

sealed class AppError(val message: String, val cause: Throwable? = null) {
    class Network(cause: Throwable?) : AppError("Нет соединения", cause)
    class Storage(msg: String, cause: Throwable?) : AppError(msg, cause)
    class Validation(msg: String) : AppError(msg)
    class Build(val phase: String, msg: String) : AppError(msg)
    class Ai(msg: String, cause: Throwable?) : AppError(msg, cause)
    class Unknown(cause: Throwable?) : AppError("Неизвестная ошибка", cause)
}
```

### Presentation Layer: UiEvent

Одноразовые события для UI (toasts, dialogs, navigation):

```kotlin
sealed class UiEvent {
    data class ShowSnackbar(val message: String, val action: String? = null) : UiEvent()
    data class ShowDialog(val title: String, val message: String) : UiEvent()
    data class NavigateTo(val route: String) : UiEvent()
    data class Error(val error: AppError) : UiEvent()
}

// В ViewModel:
private val _events = Channel<UiEvent>(Channel.BUFFERED)
val events: Flow<UiEvent> = _events.receiveAsFlow()

// В Composable:
LaunchedEffect(Unit) {
    viewModel.events.collect { event ->
        when (event) {
            is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
            is UiEvent.Error -> snackbarHostState.showSnackbar(event.error.message)
            // ...
        }
    }
}
```

**Все экраны используют этот pattern** — не каждый ViewModel изобретает свой.

### Build Errors: BuildError

Специализация `AppError` для сборки — см. `06-build-pipeline.md`.

## Crash Reporting

### MVP: Timber + local file logs

```kotlin
class AiApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(ReleaseTree(filesDir.resolve("logs")))
        }
    }
}
```

Логи пишутся в `context.filesDir/logs/app.log` (rolling, 5 MB max). Пользователь может выгрузить их через SAF.

### v0.2+: Firebase Crashlytics (opt-in)

При первом запуске — диалог:
> «Отправлять анонимные отчёты об ошибках для улучшения приложения?»

[Да] [Нет] — выбор хранится в DataStore.

**Что отправляется:** stacktrace, Android version, device model.
**Что НЕ отправляется:** код проекта пользователя, API keys, file paths с именами проектов.

## Analytics

**MVP:** нет вообще.

**v0.2+:** анонимные метрики usage (opt-in):
- Сколько проектов создано (aggregate)
- Какие виджеты используются чаще
- Время сборки (percentile)

**Что НЕ отправляем:** имена проектов, код, content.

## Security

- API keys → `EncryptedSharedPreferences` (v0.2+, в MVP — DataStore, TODO)
- Debug keystore → auto-generated, stored in `filesDir/keystore/`
- Release keystore → пользовательский через SAF (v0.2+)
- Toolchain downloads → SHA-256 checksum verification
- No dynamic code from untrusted sources (только pre-verified toolchain JARs)
- FileProvider для sharing APK с установщиком
- AI-контекст фильтруется на секреты перед отправкой (v0.2+, см. 07)
