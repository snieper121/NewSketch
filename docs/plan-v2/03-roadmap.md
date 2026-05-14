# 03 — Roadmap

## Принцип: Risk-first + реалистичные оценки

Самый большой риск — on-device компиляция. **M0 = PoC сборки, делается ПЕРВЫМ**, без UI, без JSON.

Без жёстких дедлайнов, приоритет правильности решений над скоростью.

## M0 — PoC Build Pipeline (3-5 недель)

**Цель:** собрать «Hello Compose» APK на устройстве из HARDCODED source (без JSON, без editor).

**Задачи:**
- Минимальное Android-приложение (1 Activity, 1 кнопка «Build»)
- Скачать toolchain (kotlinc.dex, compose-plugin, ecj.dex, aapt2, d8, android.jar)
- **SHA-256 verification** каждого toolchain-файла
- In-process вызов kotlinc через DexClassLoader
- ecj для компиляции R.java
- aapt2 compile + link ресурсов
- d8 dex (с multidex + desugaring если нужно)
- zipalign + apksigner (со случайным паролем для debug keystore)
- Установить собранный APK через Intent + FileProvider
- Cancellation support + cleanup tmp files

**Exit criteria:**
- [ ] Собранный APK запускается на API 29, 34, 36
- [ ] Время сборки < 5 мин на Snapdragon 6xx
- [ ] Нет OOM на 4 GB RAM
- [ ] DexClassLoader проверяет SHA-256 перед каждой загрузкой
- [ ] Cancellation cleanup работает
- [ ] Работает без root

**Результат:** Go/No-Go решение. Если не работает — пересмотр архитектуры (cloud build fallback из `09-risks.md` contingency).

---

## M1 — MVP Editor (14-22 недели суммарно)

**Цель:** пользователь создаёт 1-экранное Compose-приложение визуально и собирает APK.

### Подэтапы

**M1.1 — JSON-интеграция + Codegen (8-12 недель)**

Параллельно: M1.1 и завершение M0 integration.

- JSON-модели сохраняются в `design/*.json`
- KotlinPoet codegen из JSON → `generated/*.kt`
- Навигация через Compose Navigation 2.9 (type-safe)
- Full Material3 color scheme + typography
- State binding через `rememberSaveable`
- Expression parser с blacklist
- Генерируемый проект собирается через M0 pipeline

**M1.2 — Visual Editor (6-10 недель)**

- Canvas + 15 базовых виджетов (рекурсивный рендер WidgetNode)
- Property editor (bottom sheet)
- Undo/redo через snapshot stack
- Auto-save debounced
- Drag-and-drop reorder
- Кнопка Build → установка APK

**Exit criteria M1:**
- [ ] Создать проект → добавить виджеты → собрать → установить → работает
- [ ] Undo/redo работает
- [ ] Auto-save не теряет данные при убийстве процесса
- [ ] Тесты на API 29, 34, 36

---

## M2 — Multi-Screen + Resources (6-8 недель)

**Цель:** полноценные многоэкранные приложения с навигацией.

**Задачи:**
- Screen manager (add/remove/rename)
- Navigation Compose 2.9 codegen (type-safe routes)
- State & events (переменные, onClick → navigate/increment/etc)
- Resource manager (colors, strings, drawables + Coil для images)
- Theme customization (Material3 palette)
- Incremental build (только изменённые файлы)
- Classpath JAR caching

**Exit criteria:**
- [ ] 5-экранное приложение с навигацией собирается < 3 мин (incremental)
- [ ] Ресурсы (строки, цвета, drawables) отражаются в APK
- [ ] State management работает

---

## M3 — AI Integration (7-10 недель)

**Цель:** AI генерирует экраны и виджеты по запросу.

**Задачи:**
- AI chat UI
- Provider abstraction (OpenAI, Claude, custom)
- Context builder
- SecretFilter + TokenCounter + PromptCache
- Structured output → JSON patches
- DiffEngine с rollback + PatchApplier per type
- Diff viewer + Apply/Reject
- Usage tracking

**Exit criteria:**
- [ ] «Сделай экран логина» → AI → apply → build → работает
- [ ] Ошибки AI gracefully handled
- [ ] Контекст фильтруется на секреты

---

## M4 — Polish + Release (5-7 недель)

**Цель:** production-ready для beta-тестирования.

**Задачи:**
- Code editor (Sora или свой)
- Export → Gradle project (открывается в Android Studio)
- Onboarding flow + sample project из `assets/`
- Project settings (manifest, permissions, dependencies)
- App settings
- Error reporting (Timber/Kermit + Crashlytics opt-in)
- Performance (Baseline Profile, lazy loading, caching preview)
- Accessibility audit
- LeakCanary + StrictMode в debug

**Release plan:**
- **Alpha** (internal): конец M3
- **Closed Beta** (5-10 testers): середина M4
- **Open Beta** (Play Store beta track): конец M4
- **v1.0 stable**: +2-4 недели после Open Beta

**Exit criteria:**
- [ ] Экспортированный проект открывается и собирается в Android Studio
- [ ] Cold start < 3 сек (с Baseline Profile)
- [ ] Нет ANR при работе с 20-экранным проектом
- [ ] 5+ beta testers подтверждают работоспособность

---

## M5+ — Advanced (post-MVP, v1.0+)

- Git integration (JGit)
- Release signing (user keystore через SAF)
- Plugin system (built-in widgets API)
- Multiple projects
- Local LLM support (Ollama)
- Marketplace (post-v1.0)

---

## Таймлайн

```
Неделя:    1    5    10   15   20   25   30   35   40   45   50
           ├─M0─┤
                ├────────M1.1────────┤
                              ├──────M1.2──────┤
                                          ├─M2─┤
                                               ├───M3────┤
                                                         ├─M4─┤
Design:    ├──────────────────────────────────────────────────┤
Testing:   ├──────────────────────────────────────────────────┤
```

- **Design track:** UI/UX mockups параллельно с M0
- **Testing track:** device matrix тестирование на каждом milestone
- **Toolchain track:** CDN setup, pre-dex pipeline — во время M0

## Общий таймлайн

**Без жёстких дедлайнов, приоритет правильности решений над скоростью.**

Фазы указаны примерно, но точные сроки зависят от сложности и качества каждого решения.

## Что может изменить оценки

**Быстрее (уменьшить):**
- 2+ разработчика (M1.1 и M1.2 параллельно)
- Cloud build fallback вместо on-device (убирает M0 риски)
- Готовый Sora Editor (vs собственный)

**Медленнее (увеличить):**
- DexClassLoader проблемы на новом Android (Developer Preview)
- Compose Compiler breaking changes между Kotlin 2.1→2.2
- Google Play отклонение → redistribution на альтернативные stores
- Community contributions требуют review + onboarding
