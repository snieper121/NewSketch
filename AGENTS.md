# Ai IDE — Agent Instructions

## КРИТИЧЕСКИ ВАЖНО

- **Никогда** не запускать локальную сборку (gradle, ./gradlew, kotlinc)
- Компиляция **ТОЛЬКО** через GitHub Actions после `git push`
- Для проверки результата — смотреть CI логи на github.com
- Локально только: создание/редактирование файлов и `git push`

## Контекст проекта

Ai IDE — мобильная IDE для создания Android Compose-приложений прямо на телефоне (API 29–36+).
Пользователь создаёт Compose-приложения визуально и с помощью AI, собирает APK на устройстве без ПК.

**Репозиторий:** https://github.com/snieper121/NewSketch  
**Рабочая директория:** `/storage/emulated/0/Projects/Ai`

## Порядок чтения документов

1. `docs/plan-v2/00-vision.md` — что строим, audience, non-goals
2. `docs/plan-v2/03-roadmap.md` — milestone'ы M0–M5
3. `docs/plan-v2/10-current-state-analysis.md` — текущее состояние кода
4. `docs/plan-v2/11-next-steps.md` — конкретные шаги реализации
5. `docs/plan-v2/13-toolchain-sources-and-order.md` — порядок разработки (читать ПЕРВЫМ при старте работы)

## Текущий приоритет

**M0 PoC** — on-device компиляция Hello Compose APK.
См. `docs/plan-v2/11-next-steps.md`, раздел "Шаг 4 (M0 PoC) — ПЕРВЫМ".

## Правила работы с планом

- Перед изменением любого md-файла — прочитай связанные файлы
- Не добавляй фичи вне scope из `00-vision.md` (non-goals)
- Все предложения обосновывай ссылкой на конкретный файл плана
- При противоречиях между файлами — явно указывай конфликт
- Изменения в плане — только обоснованные, с учётом non-goals
- **Никогда** не удаляй файлы плана — только дополняй или помечай устаревшим

## Non-goals (что НЕ делаем)

- iOS / macOS / Windows / Web versions
- Kotlin Multiplatform
- Native C/C++ / Rust
- Game engines
- Backend generation
- Real-time collaboration
- Cloud project sync (только SAF)
- Release signing (debug-only до v0.2)
- Git integration (до v1.0)
- WYSIWYG pixel-perfect preview

## Стек (не менять без причины)

- Kotlin 2.2.0, Jetpack Compose, Material3
- Gradle 9.5.1 (для IDE itself), AGP 8.10.0
- DI: AppContainer (MVP), Koin в M3+
- Codegen: KotlinPoet
- Build: kotlinc + ecj + aapt2 + d8 (in-process, без Gradle для generated apps)
- Storage: app-private (filesDir), SAF для import/export
- AI: Ktor client, OpenAI/Claude compatible API

## CI/CD

- GitHub Actions: build, test, lint, assemble debug APK
- APK доступен как artifact (ZIP) в Actions
- Релизные сборки отключены до готовности MVP
- Gradle кеширование через `gradle/actions/setup-gradle@v4`

## Коммуникация

- Все обсуждения — на русском языке
- Коммит messages — на русском
- Документация — русский (primary), английский для open-source docs
