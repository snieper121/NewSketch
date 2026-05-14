# Ai IDE — Project Plan v2

Мобильная IDE для создания Android Compose-приложений на устройстве (API 29–36+).

## Документы (11)

| # | Файл | Содержание |
|---|------|-----------|
| 00 | [00-vision.md](00-vision.md) | Видение, audience, value proposition, non-goals, scope MVP/v0.2/v1.0 |
| 01 | [01-architecture.md](01-architecture.md) | Стек, single-module→multi, AppContainer→Koin, Result/UiEvent, crash reporting |
| 02 | [02-build-toolchain.md](02-build-toolchain.md) | Kotlinc+ecj+aapt2+d8, DexClassLoader, multidex, desugaring |
| 03 | [03-roadmap.md](03-roadmap.md) | Milestone'ы M0–M5, таймлайн ~20 недель |
| 04 | [04-data-model.md](04-data-model.md) | JSON-схемы, state binding, expression grammar, миграции |
| 05 | [05-code-generation.md](05-code-generation.md) | KotlinPoet, PRESERVE markers, Gradle export |
| 06 | [06-build-pipeline.md](06-build-pipeline.md) | 9 фаз сборки, cancellation, multidex, error handling |
| 07 | [07-ai-integration.md](07-ai-integration.md) | Providers, SecretFilter, TokenCounter, Prompts, DiffEngine |
| 08 | [08-testing-strategy.md](08-testing-strategy.md) | Unit+golden+snapshot+device matrix+a11y |
| 09 | [09-risks-and-compatibility.md](09-risks-and-compatibility.md) | 19 рисков, supply chain, legal, contingency plans |
| 10 | [10-current-state-analysis.md](10-current-state-analysis.md) | Что уже есть в проекте, что не хватает |
| 11 | [11-next-steps.md](11-next-steps.md) | Пошаговый план реализации (7 шагов) |

## Порядок чтения

**Для понимания проекта:**
1. `00-vision` — что строим и зачем
2. `03-roadmap` — в каком порядке
3. `11-next-steps` — конкретные шаги

**Для реализации:**
4. `10-current-state-analysis` — что уже есть
5. `01-architecture` — как устроено
6. `04-data-model` — формат данных
7. `05-code-generation` — JSON → Kotlin
8. `02-build-toolchain` — инструменты
9. `06-build-pipeline` — как собираем APK
10. `07-ai-integration` — AI-ассистент
11. `08-testing` — как проверяем
12. `09-risks` — что может пойти не так

## Первый шаг

**M0 PoC** (см. 03-roadmap, Шаг 4 из 11-next-steps): собрать Hello Compose APK на устройстве через in-process kotlinc + ecj. Без UI. Это валидирует всю архитектуру.

## Ключевые решения v2 (обновления от v1)

- **KotlinPoet** вместо string templates для codegen
- **ecj** для компиляции R.java (не kotlinc)
- **Multidex + desugaring** в pipeline
- **SecretFilter** для AI context
- **Single-module** до M3, потом разбивка
- **AppContainer** в MVP, Koin в M3+
- **Custom action запрещён** — только whitelist action types с expression grammar
- **PRESERVE маркеры** для custom code в generated files
- **Non-goals** зафиксированы в 00-vision

## Платформа

- IDE: Android 10–16 (API 29–36), forward-compatible
- Generated apps: Android 5+ (API 21+, выбирает пользователь)
- Язык: Kotlin 100%, Jetpack Compose
- Сборка: on-device, без Gradle, без root
