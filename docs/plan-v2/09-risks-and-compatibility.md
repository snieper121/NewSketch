# 09 — Risks & Compatibility

## Risk Register

| # | Риск | Вероятность | Impact | Mitigation |
|---|------|-------------|--------|-----------|
| R1 | DexClassLoader перестанет работать на API 37+ | Средняя | Критический | Мониторить Developer Preview. Fallback: cloud build. |
| R2 | OOM при компиляции на 4 GB RAM | Высокая | Высокий | Chunked compilation (1-2 файла за раз), `-Xmx1g` |
| R3 | aapt2/zipalign не запускается на новом API | Низкая | Высокий | Обновлять Build-Tools до latest, тестировать на DP |
| R4 | Kotlin/Compose breaking changes | Средняя | Средний | Пинить версии, обновлять раз в квартал |
| R5 | Google Play отклонит из-за dynamic code loading | Средняя | Критический | Аргументация: собственный toolchain, не чужой код. Appeal готов. |
| R6 | Thermal throttling делает сборку >30 мин | Средняя | Средний | Паузы между фазами, предупреждение пользователю |
| R7 | AI provider меняет API/pricing | Низкая | Низкий | Абстракция провайдеров, поддержка local LLM |
| R8 | Compose classpath несовместим с новым BOM | Средняя | Средний | Toolchain manifest versioning, тестирование перед обновлением |
| R9 | Foreground service restrictions на API 37+ | Средняя | Высокий | Мониторить. Альтернатива: WorkManager long-running |
| R10 | Размер APK IDE >150 MB (Play Store limit 200 MB) | Низкая | Средний | Toolchain — lazy download, не в APK |
| R11 | Supply chain attack (compromised dependency) | Низкая | Критический | SHA-256 lock, SBOM, периодический аудит |
| R12 | Legal: AI отправляет код в США/другие юрисдикции | Средняя | Высокий | Явное уведомление, opt-in, local LLM опция, GDPR compliance |
| R13 | User data leak через AI context | Низкая | Критический | SecretFilter, явный consent, audit logs |
| R14 | ecj licensing (EPL) ограничения | Низкая | Низкий | EPL совместим с нашим кодом (dynamic linking), проверено |
| R15 | KotlinPoet breaking changes | Низкая | Низкий | Пинить версию, unit tests ловят регрессии |
| R16 | Play Store policy на AI-generated content | Средняя | Средний | Мониторить policy, опция отключить AI |
| R17 | Local LLM требует слишком много памяти | Высокая | Низкий | MVP только cloud, local в v1.0 с явным hardware req |
| R18 | CDN для toolchain недоступен | Низкая | Критический | Multi-CDN (AWS + Google Cloud), fallback на Maven Central |
| R19 | Государственный бан Play Store в некоторых странах | Высокая | Средний | Alternative distribution: APK на сайте, F-Droid, Huawei |
| R20 | GitHub token не персистится между сессиями | Средняя | Низкий | Документировать в AGENTS.md процесс авторизации |
| R21 | Kotlin 2.2.0 + Compose Compiler Plugin совместимость не протестирована | Средняя | Высокий | Первый CI-прогон после каждого обновления Kotlin |
| R22 | GitHub Actions runner на x86_64, но мы на aarch64 — нет локальной валидации | Средняя | Средний | Всегда дожидаться CI, никогда не мержить без прохождения |
| R23 | Play Store может отклонить из-за dynamic code loading (DexClassLoader) | Средняя | Критический | Подготовить appeal letter, альтернатива: bundled toolchain |
| R24 | 130MB toolchain download на мобильном соединении без resume | Высокая | Средний | Реализовать HTTP Range requests, chunked download |
| R25 | AGP 8.10.0 + Gradle 9.5.1 — нестандартная комбинация | Низкая | Средний | Мониторить release notes, держать fallback конфигурацию |
| R26 | EncryptedSharedPreferences в security-crypto 1.1.0-alpha06 — alpha quality | Средняя | Средний | Обёртка с fallback на обычный DataStore при crash |

## Forward-Compatibility Policy

### Target SDK Update Cycle

Google Play требует `targetSdk ≥ latest - 1` в течение года после релиза нового API.

**Наш цикл:**
1. **Февраль–март:** Android Developer Preview выходит → начинаем тестирование IDE
2. **Июнь–август:** Beta → фиксим breaking changes
3. **Сентябрь–октябрь:** Stable release → обновляем targetSdk
4. **Ноябрь:** публикуем обновление в Play Store

### Что тестируем на каждом DP

- [ ] DexClassLoader загружает kotlinc.dex
- [ ] DexClassLoader загружает ecj.dex
- [ ] ProcessBuilder запускает aapt2
- [ ] Foreground service работает с правильным type
- [ ] FileProvider sharing APK работает
- [ ] Scoped storage не сломан
- [ ] Edge-to-edge UI корректен
- [ ] Notifications requesting permission работает

### Toolchain Update Policy

| Компонент | Частота обновления | Триггер |
|-----------|-------------------|---------|
| Kotlin + Compose Plugin | Раз в квартал | Stable release |
| Compose BOM (classpath) | Раз в квартал | Stable BOM |
| Build-Tools (aapt2, d8) | Раз в год | Новый targetSdk |
| android.jar | Раз в год | Новый platform API |
| ecj | Раз в год или при bug fix | EPL release |
| KotlinPoet (IDE-side) | Ad-hoc | Breaking fix нужен |

### Runtime Feature Detection

Везде `Build.VERSION.SDK_INT` вместо хардкода:

```kotlin
// Foreground service
if (Build.VERSION.SDK_INT >= 34) {
    startForeground(id, notification, FOREGROUND_SERVICE_TYPE_DATA_SYNC)
} else {
    startForeground(id, notification)
}

// Notifications
if (Build.VERSION.SDK_INT >= 33) {
    requestPermission(POST_NOTIFICATIONS)
}
```

## Google Play Compliance

### Dynamic Code Loading

Google Play policy (2024+): запрещает загрузку исполняемого кода из сторонних источников.

**Наш случай — допустим:**
- Мы загружаем **собственный** toolchain (kotlinc, ecj, d8) с **нашего** CDN
- Это не «чужой код» — это часть функциональности приложения
- Аналоги (AIDE, AndroidPE, Dcoder) присутствуют в Play Store

**Подстраховка:**
- В описании приложения явно указать: «IDE для разработки Android-приложений, включает компилятор»
- Подготовить appeal letter с объяснением и примерами аналогов
- Альтернатива: bundled в APK (увеличит размер до ~180 MB, но в пределах лимита)

### Foreground Service Justification

API 34+ требует декларацию `foregroundServiceType`. Для компиляции — `dataSync`.

Обоснование для Play Store:
> «Foreground service необходим для долгой компиляции проекта пользователя на устройстве (5-15 минут). Пользователь явно запускает сборку, видит notification с прогрессом.»

Backup plan: `WorkManager` с `setExpedited()` + notification (если FGS запретят).

### AI-Generated Content

Play Policy требует:
- Четкое указание «содержит AI-функции»
- User ability to opt-out AI features
- Filtering for harmful content

**Наш compliance:**
- В Settings: toggle «AI Assistant» (по умолчанию off)
- System prompt запрещает generating harmful code
- UI label: «✨ AI» у AI-related features

## Supply Chain Security

### Угрозы
- Compromised dependency в Maven Central
- MitM при скачивании toolchain
- Typosquatting пакетов
- Maintainer account takeover

### Mitigation

**Build-time (IDE):**
- `libs.versions.toml` с жёстко зафиксированными версиями (нет `+` или `[1.0,)`)
- Gradle verification metadata (`gradle/verification-metadata.xml`) с SHA-256 для всех зависимостей
- Dependabot / Renovate для отслеживания обновлений с manual review
- Periodic SBOM generation (`./gradlew :app:dependencyReport`)

**Runtime (toolchain download):**
- SHA-256 verification из signed manifest.json
- HTTPS only, certificate pinning для CDN
- Fallback на Maven Central (signed by JetBrains/Google) если свой CDN compromised

**Code provenance:**
- Reproducible builds (опционально, для open-source release)
- Sign releases with GPG
- Tag releases с known-good commits

## Privacy & Legal

### Data handling

| Данные | Хранение | Отправка |
|--------|----------|----------|
| Код проекта (Kotlin) | Локально (filesDir) | Только в AI context (opt-in) |
| JSON-модель проектов | Локально | Никогда |
| API keys | EncryptedSharedPreferences | К AI provider only |
| User preferences | DataStore | Никогда |
| Crash logs | Локально | Firebase Crashlytics (opt-in, v0.2+) |
| Usage metrics | Локально | Aggregate only, opt-in (v0.2+) |

### Legal compliance

**GDPR (EU users):**
- Явный consent при первом использовании AI
- Право на удаление (`Settings → Clear all data`)
- Data export (проекты через SAF, settings через backup)
- Privacy policy в Settings

**Российские требования (152-ФЗ):**
- Локальное хранение persondata (у нас нет persondata пользователей — только код)
- Опционально: локальный LLM (Ollama) для compliance

**Google Play Data Safety:**
- Declare: Device IDs (none), Location (none), Personal info (none), Financial info (none)
- Declare: App activity (usage metrics, opt-in)

### AI & user code

**Policy:**
- Код пользователя НЕ используется для тренировки моделей
- OpenAI/Anthropic ToS на момент релиза это гарантируют для API (не для ChatGPT/Claude.ai)
- Пользователь информируется при первом использовании AI

**Запрет:**
- Не отправлять код если detectи secrets (SecretFilter block)
- Не логировать отправляемый context

## Dependency Licenses

| Зависимость | Лицензия | Ок для коммерции |
|-------------|----------|-----------------|
| Kotlin compiler | Apache 2.0 | ✅ |
| Compose compiler plugin | Apache 2.0 | ✅ |
| ecj (Eclipse JDT) | EPL 2.0 | ✅ (dynamic linking) |
| android.jar | Apache 2.0 | ✅ |
| aapt2 | Apache 2.0 | ✅ |
| d8/R8 | Apache 2.0 | ✅ |
| KotlinPoet | Apache 2.0 | ✅ |
| AndroidX libraries | Apache 2.0 | ✅ |
| Sora Editor | LGPL 2.1 | ⚠️ Dynamic linking OK |
| Ktor | Apache 2.0 | ✅ |
| Koin | Apache 2.0 | ✅ |
| Timber | Apache 2.0 | ✅ |
| tiktoken Kotlin port | MIT | ✅ |

**Sora Editor (LGPL):** допустимо если используется как отдельная библиотека (не модифицируем исходники). Альтернатива: CodeView (MIT).

**ecj (EPL 2.0):** требует явного указания «Uses Eclipse JDT Compiler, EPL 2.0». Добавить в Settings → About.

## Security Risks

| Риск | Mitigation |
|------|-----------|
| MitM при скачивании toolchain | SHA-256 checksum + HTTPS only + cert pinning |
| Malicious AI response (code injection) | Patches применяются к JSON, не к raw code. Валидация перед apply. Expression grammar ограничена. |
| User API key leak | EncryptedSharedPreferences, не логируем, не в analytics |
| Debug keystore compromise | Keystore в app-private, не экспортируется |
| Malicious project import | Валидация JSON schema перед loading, sandbox generated apps |
| Unsafe links в generated apps | Generated app имеет только permissions из project.json (whitelist) |
| Secrets в project code → AI | SecretFilter + user confirmation |
| Supply chain | SHA-256 lock + reproducible builds + audit |

## Contingency Plans

### Если DexClassLoader заблокируют (API 37+)

1. **Cloud build:** отправить .kt файлы на сервер → получить APK
2. **Embedded compiler:** включить kotlinc прямо в APK как DEX (не через DexClassLoader, а как обычную зависимость). Увеличит APK на ~60 MB.
3. **Termux-подход:** отдельный процесс с собственным runtime

### Если Google Play отклонит

1. Распространение через: собственный сайт (APK), F-Droid, Huawei AppGallery, Samsung Galaxy Store
2. Убрать dynamic download, bundled всё в APK (~180 MB)
3. Appeal с примерами аналогов (AIDE, Dcoder, AndroidPE)

### Если kotlinc слишком медленный на устройстве

1. Cloud compilation (hybrid): тяжёлые проекты → сервер, лёгкие → on-device
2. Ограничить размер проекта (max 10 экранов для on-device)
3. Более агрессивный incremental build (statement-level, не file-level)

### Если CDN недоступен

1. Fallback chain: primary CDN → secondary CDN → Maven Central + Google Maven
2. User может указать свой mirror в Settings
3. Offline mode: работа с уже скачанным toolchain

### Если AI provider недоступен

1. Provider abstraction позволяет переключить в Settings
2. Supported providers: OpenAI, Anthropic, OpenRouter, Ollama (local)
3. Non-AI workflow: всё остальное работает без AI

### Если юридический ban (privacy law)

1. Local LLM mode (Ollama) — никуда не отправляем данные
2. Self-hosted Ollama endpoint для enterprise
3. Region-specific version с disabled AI

## Monitoring & Response

**Post-release monitoring:**
- Crashlytics (opt-in): отслеживание crash rate по API levels
- GitHub Issues: community bug reports
- Play Store reviews: monitoring негативных отзывов

**Security incident response:**
1. Detection (user report, automated scan, news) → 24 hours
2. Assessment — severity (Critical/High/Medium/Low)
3. Mitigation (hotfix / rollback)
4. Disclosure — CVE + security advisory (для Critical/High)
5. Post-mortem + процедура обновить

**SLA для Critical issues:**
- Public disclosure: 90 дней после обнаружения (coordinated)
- Hotfix release: 7 дней для Play Store
