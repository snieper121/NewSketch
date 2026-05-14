# 12 — Additional Concerns

## 1. UX Details

- **Transitions:** shared element transitions между ProjectsList → Editor
- **Gestures:** long-press виджет → context menu (delete/duplicate/move), swipe-to-delete в списках
- **Haptic:** вибрация при drop виджета, при ошибке валидации
- **Keyboard:** hardware keyboard shortcuts (Ctrl+S save, Ctrl+Z undo, Ctrl+B build)
- **Landscape:** editor split view (canvas left, properties right)

## 2. Onboarding + Sample Projects

**Первый запуск:**
1. Welcome screen (3 slides: что это, как работает, начать)
2. Предложить открыть sample project (bundled в `assets/sample-hello/`)
3. Guided tour: подсветка кнопок «Добавить виджет» → «Build»

**Sample templates (design/*.json bundled):**
- **Empty** — Scaffold + Text "Hello"
- **Counter** — Button + Text + state counter
- **Login** — 2 TextField + Button + navigate
- **List** — LazyColumn + Card items

## 3. Undo/Redo

Stack-based:
```kotlin
class UndoManager(private val maxHistory: Int = 50) {
    private val undoStack = ArrayDeque<ScreenModel>()
    private val redoStack = ArrayDeque<ScreenModel>()
    fun push(state: ScreenModel) { undoStack.addLast(state); redoStack.clear() }
    fun undo(current: ScreenModel): ScreenModel? { /* ... */ }
    fun redo(current: ScreenModel): ScreenModel? { /* ... */ }
}
```

## 4. Auto-save

- Debounce 500 ms после последнего изменения
- Пишет JSON в `design/screens/<id>.json`
- При process death — `rememberSaveable` сохраняет UI state, JSON уже на диске

## 5. Disk Space Handling

- Перед download toolchain: проверить `StatFs(filesDir).availableBytes > required * 1.2`
- Перед build: проверить свободное место > 100 MB
- При нехватке: `UiEvent.ShowDialog("Недостаточно места")`

## 6. Hardware Requirements (документировать)

| | Минимум | Рекомендуется |
|---|---|---|
| RAM | 4 GB | 6+ GB |
| Storage | 500 MB свободно | 1+ GB |
| CPU | ARM64 | Snapdragon 6xx+ |
| Android | 10 (API 29) | 13+ (API 33+) |

## 7. networkSecurityConfig (K8)

Для Ollama (localhost HTTP):
```xml
<!-- res/xml/network_security_config.xml -->
<network-security-config>
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="false">localhost</domain>
        <domain includeSubdomains="false">127.0.0.1</domain>
    </domain-config>
</network-security-config>
```
Manifest: `android:networkSecurityConfig="@xml/network_security_config"`

## 8. Permissions UX (generated apps)

В Project Settings → Permissions:
- Checkboxes для common permissions (INTERNET, CAMERA, LOCATION, ...)
- Dangerous permissions показывают warning icon
- Генерируется в AndroidManifest.xml

## 9. Custom Dependencies UX (v0.2+)

- Текстовое поле: `group:artifact:version`
- Валидация формата
- **MVP:** только whitelist (Compose BOM, Navigation, Coil)
- **v0.2+:** Maven Central resolver (HEAD request для проверки существования)
- Транзитивные зависимости НЕ резолвятся (только direct)

## 10. AI Cost Budgeting

Settings → AI → Budget:
- Daily limit: $0 (unlimited) / $0.50 / $1 / $5 / custom
- Warning at 80% of limit
- Hard stop at 100% (показать сообщение, не отправлять)
- Reset daily at midnight local time

## 11. Error Recovery

- Build crash → `build/` dir cleanup при следующем запуске
- Corrupted JSON → `SchemaMigrator` пытается починить, если нет → backup + reset
- Toolchain corrupted → SHA-256 mismatch → auto-redownload
- Crash log → `filesDir/logs/crash_<timestamp>.txt` → экспорт через SAF

## 12. Contributor Experience

`CONTRIBUTING.md`:
- Setup: JDK 17, Android SDK 36, clone, `./gradlew assembleDebug`
- Code style: ktlint, 4-space indent, trailing comma
- PR process: fork → branch → PR → 1 review → merge
- Commit messages: conventional commits (`feat:`, `fix:`, `docs:`)
- Architecture: read `docs/plan-v2/01-architecture.md` first

## 13. Deployment Process

- **versionCode:** auto-increment from git tag count
- **versionName:** semver `0.1.0-alpha.1`
- **Signing:** release keystore in CI secrets (GitHub Actions)
- **Play Store:** internal → closed beta → open beta → production
- **Changelog:** `CHANGELOG.md` (keep-a-changelog format)

## 14. Beta/Feedback Loop

- Google Play Internal Testing (up to 100 testers)
- In-app feedback button (Settings → Send Feedback → email/GitHub issue)
- Crash reports via Crashlytics (opt-in)
- GitHub Discussions for feature requests
- Monthly survey (Google Forms) for beta testers
