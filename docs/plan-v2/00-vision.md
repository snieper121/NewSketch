# 00 — Видение проекта Ai IDE

## Что это

Ai IDE — мобильная среда разработки Android-приложений, работающая на Android-устройствах (API 29–36+). Пользователь создаёт Compose-приложения визуально и с помощью AI, собирает APK на устройстве без ПК.

## Аудитория и value proposition

**Целевая аудитория:**
- **Начинающие разработчики** — учатся Kotlin/Compose через визуальное создание
- **Hobbyists** — делают приложения для себя без ПК
- **Prototypers** — быстро собирают MVP для проверки идей
- **Teachers / students** — обучение мобильной разработке прямо с телефона/планшета

**Чего НЕ целевая аудитория:**
- Профессиональные разработчики enterprise-приложений (для них Android Studio + ПК)
- Команды с CI/CD, большими кодовыми базами
- Разработчики приложений с сложной логикой (игры, сложные расчёты, ML)

**Value proposition:**
1. **Единственное устройство** — телефон/планшет вместо ноутбука + IDE + SDK
2. **AI-first** — генерация экранов и логики по запросу
3. **Compose-native** — современный стек, без легаси XML
4. **Без barriers** — нет настройки Gradle, SDK, эмуляторов

## Платформенные требования

| Параметр | Значение |
|----------|----------|
| IDE minSdk | 29 (Android 10) |
| IDE targetSdk | 36 (Android 16), обновляется ежегодно |
| Generated apps minSdk | 21+ (выбирает пользователь) |
| Generated apps targetSdk | latest stable (по умолчанию) |
| Forward-compat | тестирование на каждом Developer Preview |

## Парадигма

- **Compose-only** — никаких XML-layout, всё UI — декларативный Kotlin
- **JSON — source of truth** — визуальный редактор работает с JSON-моделью, из неё генерируется Kotlin-код
- **On-device build** — компиляция без Gradle, через in-process вызов kotlinc/ecj/d8/aapt2
- **AI co-pilot** — генерация экранов, виджетов, логики через чат с контекстом проекта

## Хранение данных

Все проекты и toolchain хранятся в **app-private** директории (`context.filesDir`). Импорт/экспорт — через SAF (Storage Access Framework). Это обеспечивает работу на всех версиях Android 10–16 без специальных разрешений.

## Главные экраны

1. **Home** — список проектов, создание нового
2. **Project Editor** — визуальный редактор + код + палитра виджетов
3. **Property Editor** — свойства выбранного виджета (bottom sheet)
4. **Screen Manager** — список экранов, навигация
5. **Resource Manager** — цвета, строки, drawables, шрифты
6. **State & Logic** — переменные, события, функции
7. **Build & Run** — сборка APK, лог, установка
8. **AI Chat** — генерация с diff-preview и apply/reject
9. **Settings** — AI provider, toolchain, тема
10. **Project Settings** — manifest, permissions, dependencies

## User Journey (основной)

1. Создаёт проект (имя, package, template)
2. Добавляет виджеты через палитру или AI
3. Настраивает свойства, события, навигацию
4. Собирает APK (5–15 мин на устройстве)
5. Устанавливает и запускает

## Non-goals (что мы НЕ делаем)

Чтобы избежать scope creep, явно фиксируем что **вне скоупа**:

### Никогда не делаем
- **iOS / macOS / Windows / Web versions** — только Android
- **Kotlin Multiplatform** — Compose для Android, не KMP
- **Native C/C++ / Rust** — только JVM
- **Game engines** — не Unity, не Godot, не интерактивная графика
- **Backend generation** — только клиент, никаких серверов/баз

### Не в MVP (возможно в v1.0+)
- **Real-time collaboration** — multi-user, live sharing
- **Cloud project sync** — версии/бэкапы только через SAF
- **Database designer** — Room/SQL через визуальный редактор
- **Complex animation editor** — basic animations OK, визуальный таймлайн — нет
- **Marketplace плагинов** — post-v1.0
- **Release signing** — debug-only в MVP, release с v0.2
- **Git integration** — с v1.0 (JGit)
- **Custom Kotlin actions в JSON** — в MVP `action.type = "custom"` запрещён

### Не делаем специально
- **WYSIWYG pixel-perfect preview** — используем JSON-based rendering, не hot reload реальной компиляции
- **Полная поддержка всех Compose API** — только курированный каталог виджетов
- **Поддержка Dagger/Hilt в generated apps** — встроенный DI pattern или Koin (опционально)

## Принципы

1. **Mobile-first** — всё управляется пальцами, gesture-based
2. **Offline-first** — работа без сети (AI требует сеть или local LLM)
3. **Material3** — dynamic colors, dark mode, accessibility
4. **Incremental** — пересобирается только изменённое
5. **Safe** — app-private storage, no root, encrypted API keys

## Локализация

- **UI IDE:** русский (primary) + английский (v0.2)
- **Generated apps:** пользователь выбирает (strings.xml / resources/strings.json)
- **AI responses:** на языке запроса пользователя

## Монетизация (модель)

**MVP (v0.1 — v1.0):** полностью бесплатно, open source (Apache 2.0).

**v1.0+:**
- Core IDE остаётся бесплатным + open source
- Optional: subscription для hosted AI (если свой LLM-endpoint)
- Optional: premium plugins в marketplace

## Scope MVP (v0.1)

- 1 проект, до 5 экранов
- 15 базовых виджетов (Text, Button, Column, Row, Card, TextField, Image, Icon, Scaffold, TopAppBar, LazyColumn, LazyRow, Spacer, Divider, Switch)
- Сборка debug APK на устройстве
- Без AI (добавляется в v0.2)
- Без плагинов/маркетплейса (v1.0+)
- Без git, без release signing

## Scope v0.2

- AI-чат (OpenAI/Claude)
- Неограниченные экраны
- Ресурсы (colors, strings, drawables)
- Export в Gradle-проект
- Release signing (user keystore через SAF)

## Scope v1.0

- Plugin system (built-in only)
- Git integration (JGit)
- Multiple projects open simultaneously
- Marketplace (post-v1.0)
- Local LLM support (Ollama)

## Key assumptions

Риски, которые считаем допустимыми (детально в `09-risks-and-compatibility.md`):

1. **DexClassLoader + kotlinc работает на API 29–36** — базовое предположение. Валидируется в M0 PoC.
2. **Compose-проект компилируется за ≤15 мин на устройстве 6 GB RAM** — реалистично для 5-экранного приложения.
3. **Google Play допускает dynamic toolchain** — аналоги (AIDE, AndroidPE) существуют.
4. **Пользователи готовы скачать ~130 MB toolchain** — lazy download после первого build.
