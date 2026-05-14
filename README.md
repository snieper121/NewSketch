# Ai IDE

Мобильная среда разработки Android Compose-приложений. Создавай, редактируй и собирай APK прямо на телефоне — без ПК.

## Возможности

- **Визуальный редактор** — drag-and-drop виджетов Compose
- **AI-ассистент** — генерация экранов и логики по запросу
- **On-device build** — сборка APK без Gradle, без root
- **Material3** — dynamic colors, dark mode
- **Export** — открывается в Android Studio как стандартный Gradle-проект

## Требования

- Android 10+ (API 29)
- 4 GB RAM минимум (рекомендуется 6+)
- ~200 MB свободного места (приложение + toolchain)
- Интернет для первого запуска (скачивание toolchain ~136 MB)

## Сборка проекта

```bash
./gradlew :app:assembleDebug
```

Требуется JDK 17+, Android SDK с compileSdk 36.

## Документация

Полный план проекта: [`docs/plan-v2/README.md`](docs/plan-v2/README.md)

## Стек

- Kotlin 2.1.0, Jetpack Compose, Material3
- Room, Ktor, Kotlinx Serialization, KotlinPoet
- On-device: kotlinc, ecj, aapt2, d8, apksigner (in-process via DexClassLoader)

## Лицензия

[Apache License 2.0](LICENSE)
