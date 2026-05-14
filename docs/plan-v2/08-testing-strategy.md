# 08 — Testing Strategy

## Принцип

Главный риск — on-device компиляция. Тестируем её первой и на реальных устройствах. Codegen покрываем golden file tests. UI тесты — снапшотами.

## Уровни тестирования

| Уровень | Что | Инструмент | Когда |
|---------|-----|-----------|-------|
| Unit | Codegen, JSON parsing, diff engine, expression parser | JUnit 5 + Turbine + AssertK | Каждый PR |
| Golden file | Codegen вывод → референсный Kotlin | JUnit + assertions | Каждый PR |
| Integration | Build pipeline end-to-end | AndroidX Test + device | Каждый milestone |
| UI snapshot | Экраны визуально | Paparazzi / Roborazzi | Каждый milestone |
| UI behavior | Compose testing | `composeTestRule` | M1+ |
| Device matrix | Совместимость API 29–36 | Firebase Test Lab | Каждый release |
| Performance | Время сборки, RAM, thermal | Macrobenchmark | M1+ |
| Accessibility | TalkBack, contrast | Accessibility Scanner | Каждый release |

## Unit Tests

Выполняются на JVM (без Android device). Покрывают:

### Codegen (JUnit + Turbine)

```kotlin
class ScreenGeneratorTest {
    @Test fun `generates valid Kotlin from minimal screen json`()
    @Test fun `generates navigation for multi-screen project`()
    @Test fun `handles empty children list`()
    @Test fun `escapes special characters in string literals`()
    @Test fun `generates state binding for TextField`()
    @Test fun `generates setState action correctly`()
}
```

### Data model

```kotlin
class ProjectModelTest {
    @Test fun `migrates v1 schema to v2`()
    @Test fun `validates widget id uniqueness`()
    @Test fun `detects circular navigation`()
    @Test fun `rejects unknown action types`()
    @Test fun `rejects custom action in MVP mode`()
}
```

### Expression parser

```kotlin
class ExpressionParserTest {
    @Test fun `parses simple arithmetic`() {
        val ast = parser.parse("counter + 1")
        assertThat(ast).isEqualTo(Binary(Plus, Ref("counter"), IntLit(1)))
    }
    @Test fun `rejects function call`() {
        assertThat(runCatching { parser.parse("foo()") }).isFailure()
    }
    @Test fun `allows boolean logic`()
    @Test fun `allows ternary if`()
}
```

### AI

```kotlin
class ResponseParserTest {
    @Test fun `parses valid patch response`()
    @Test fun `handles malformed JSON gracefully`()
    @Test fun `extracts JSON from markdown code block`()
    @Test fun `handles tool_call format`()
}

class SecretFilterTest {
    @Test fun `redacts OpenAI api key`()
    @Test fun `redacts Anthropic api key`()
    @Test fun `redacts password in config`()
    @Test fun `preserves non-sensitive text`()
}
```

### Diff engine

```kotlin
class DiffEngineTest {
    @Test fun `applies add_screen patch`()
    @Test fun `applies remove_widget patch`()
    @Test fun `rollback restores original state`()
    @Test fun `skips patch referencing missing widget`()
}
```

**Цель покрытия:** codegen 90%+, diff engine 90%+, expression parser 95%+, остальное 70%+.

## Golden File Tests

Для codegen — **критически важны**. Детерминированный output KotlinPoet делает это надёжным.

```kotlin
@RunWith(Parameterized::class)
class CodegenGoldenTest(private val caseName: String) {

    @Test
    fun `generated code matches golden`() {
        val input = loadResource("codegen/$caseName/input.json")
        val expected = loadResource("codegen/$caseName/expected.kt")
        val actual = ProjectGenerator.generateScreen(
            screen = Json.decodeFromString(input),
            packageName = "com.test"
        )
        assertThat(actual).isEqualTo(expected)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases() = listOf(
            "counter_basic",
            "login_form",
            "list_with_scaffold",
            "nested_containers",
            "all_modifiers",
            "edge_empty_screen"
        )
    }
}
```

Структура:
```
src/test/resources/codegen/
├── counter_basic/
│   ├── input.json
│   └── expected.kt
├── login_form/
│   ├── input.json
│   └── expected.kt
```

**Обновление golden files:** gradle task `./gradlew :app:updateGoldenFiles` запускает тесты в update mode и записывает `actual` как новый `expected`. Ревьюеры проверяют diff.

## Integration Tests (Build Pipeline)

Запускаются на реальном устройстве/эмуляторе. Проверяют полный цикл:

```kotlin
@RunWith(AndroidJUnit4::class)
class BuildPipelineTest {

    @Test fun compileHelloWorldCompose() {
        val project = createMinimalProject()
        val result = runBlocking { buildPipeline.build(project).last() }
        assertThat(result).isInstanceOf(BuildProgress.Success::class)
        val apk = (result as BuildProgress.Success).apkFile
        assertThat(apk).exists()
        assertThat(verifySignature(apk)).isTrue()
        assertThat(apkContainsClass(apk, "com.test.MainActivity")).isTrue()
    }

    @Test fun incrementalBuildFasterThanFull() {
        val project = createFiveScreenProject()
        val fullTime = measureTimeMillis { runBlocking { buildPipeline.build(project).last() } }
        modifyOneScreen(project)
        val incrTime = measureTimeMillis { runBlocking { buildPipeline.buildIncremental(project).last() } }
        assertThat(incrTime).isLessThan(fullTime / 2)
    }

    @Test fun buildFailsGracefullyOnSyntaxError() {
        val project = createProjectWithBadExpression("invalid_expr!!")
        val result = runBlocking { buildPipeline.build(project).last() }
        assertThat(result).isInstanceOf(BuildProgress.Error::class)
    }

    @Test fun buildCancellationCleansUpTempFiles() {
        val project = createMinimalProject()
        val job = launch { buildPipeline.build(project).collect {} }
        delay(2000)
        job.cancelAndJoin()
        val tempDir = File(project.buildDir, "tmp")
        assertThat(tempDir.listFiles()).isEmpty()
    }
}
```

## UI Snapshot Tests (Paparazzi / Roborazzi)

Снимки Compose-экранов на JVM (без device):

```kotlin
class ProjectsScreenSnapshotTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_6)

    @Test fun empty_state() {
        paparazzi.snapshot {
            AiAppTheme {
                ProjectsScreen(state = ProjectsUiState(projects = emptyList()))
            }
        }
    }

    @Test fun populated_list() {
        paparazzi.snapshot {
            AiAppTheme {
                ProjectsScreen(state = ProjectsUiState(projects = sampleProjects(5)))
            }
        }
    }

    @Test fun create_dialog_open() { ... }
}
```

Снимки коммитятся в репо. CI сравнивает с референсом, failing diff → артефакт в отчёте.

## UI Behavior Tests (Compose testing)

```kotlin
@RunWith(AndroidJUnit4::class)
class EditorScreenTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test fun addingWidgetAppearsOnCanvas() {
        composeTestRule.setContent {
            AiAppTheme { EditorScreen(viewModel = fakeViewModel()) }
        }
        composeTestRule.onNodeWithContentDescription("Меню").performClick()
        composeTestRule.onNodeWithText("Button").performClick()
        composeTestRule.onNodeWithText("Button").assertIsDisplayed()
    }
}
```

## Device Matrix

| API | Устройство | Обязательно | Фокус |
|-----|-----------|-------------|-------|
| 29 | Emulator / Pixel 3a | да | minSdk boundary, scoped storage |
| 30 | Emulator | да | package visibility, scoped storage enforcement |
| 33 | Emulator | smoke | notifications permission |
| 34 | Emulator / physical | **критично** | W^X, foreground service type, DexClassLoader |
| 35 | Emulator / physical | **критично** | 16-KB pages, edge-to-edge |
| 36 | Emulator / Pixel 8+ | да | targetSdk, latest restrictions |

**Минимум для CI:** API 29, 34, 36.

**Physical devices:** минимум 2 — бюджетный (4 GB RAM, Snapdragon 6xx) и флагман.

## PoC Validation Checklist (M0)

Перед продолжением разработки — все пункты должны быть ✅:

- [ ] kotlinc.dex загружается через DexClassLoader на API 29
- [ ] kotlinc.dex загружается через DexClassLoader на API 34
- [ ] kotlinc.dex загружается через DexClassLoader на API 36
- [ ] ecj.dex компилирует R.java → .class
- [ ] Compose plugin применяется корректно (@Composable компилируется)
- [ ] aapt2 native binary запускается на API 35 (16-KB aligned)
- [ ] d8 dex'ит скомпилированные .class файлы
- [ ] d8 multidex работает (Compose проект превышает 65k methods)
- [ ] d8 desugaring работает для minSdk < 26
- [ ] Собранный APK устанавливается через FileProvider
- [ ] Собранный APK запускается и показывает Compose UI
- [ ] Нет OOM на устройстве с 4 GB RAM
- [ ] Время сборки Hello World < 5 мин
- [ ] Build cancellation работает корректно (cleanup tmp files)

## Performance Benchmarks

```kotlin
@RunWith(AndroidJUnit4::class)
class BuildBenchmark {
    @get:Rule val rule = MacrobenchmarkRule()

    @Test fun buildSmallProject() = rule.measureRepeated(
        packageName = "my.company.ai",
        metrics = listOf(FrameTimingMetric(), MemoryCountersMetric()),
        iterations = 3,
        compilationMode = CompilationMode.Partial()
    ) {
        startActivityAndWait()
        // trigger build action
    }
}
```

**Целевые метрики:**

| Метрика | Target | Acceptable |
|---------|--------|-----------|
| Build 1 screen (full) | < 3 min | < 5 min |
| Build 5 screens (full) | < 8 min | < 15 min |
| Build 5 screens (incremental) | < 90 sec | < 3 min |
| IDE cold start | < 3 sec | < 5 sec |
| Widget add → preview update | < 200 ms | < 500 ms |
| IDE RAM usage (idle) | < 300 MB | < 512 MB |
| Build RAM peak | < 1.2 GB | < 1.5 GB |
| AI response first chunk | < 2 sec | < 5 sec |

## Accessibility Tests

```kotlin
@RunWith(AndroidJUnit4::class)
class AccessibilityTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test fun allButtonsHaveContentDescription() {
        composeTestRule.setContent { AiAppTheme { ProjectsScreen(...) } }
        composeTestRule.onAllNodes(isButton())
            .assertAll(hasContentDescription().or(hasText()))
    }

    @Test fun textHasMinimumContrast() {
        // Использует Accessibility Scanner через macrobenchmark
    }
}
```

Плюс ручной audit через Accessibility Scanner на каждом release.

## AI Response Quality Tests

Не детерминированные, но важные. Curated test set:

```kotlin
class AiQualityTest {
    @Test fun `generates login screen with required fields`() = runTest {
        val response = ai.chat("Сделай экран логина с email и паролем")
        val patches = parser.parse(response).patches
        assertThat(patches).contains<AddScreen>()
        val screen = (patches.first() as AddScreen).screen
        assertThat(screen.findWidgets("TextField")).hasSize(2)
        assertThat(screen.findWidgets("Button")).hasSize(1)
    }
}
```

Запускаются против реального AI endpoint (с фиксированным seed/temperature=0). Cost монитороится.

## CI Pipeline

```yaml
# .github/workflows/ci.yml
jobs:
  unit:
    steps:
      - checkout
      - setup-jdk-17
      - run: ./gradlew test                # unit + golden
      - run: ./gradlew verifyPaparazzi     # snapshot

  instrumented:
    runs-on: macos-latest  # для Android emulator
    strategy:
      matrix: { api: [29, 34, 36] }
    steps:
      - checkout
      - setup-jdk-17
      - uses: reactivecircus/android-emulator-runner
        with:
          api-level: ${{ matrix.api }}
          script: ./gradlew connectedCheck

  benchmark:
    if: github.event_name == 'release'
    steps:
      - run: ./gradlew :app:benchmarkRelease
```

Инструменты:
- GitHub Actions (free tier для open source)
- Firebase Test Lab (full device matrix на release)

## Manual QA Checklist (per milestone)

- [ ] Создать проект → 3 экрана → build → install → navigate between screens
- [ ] Добавить все 15 виджетов → build → verify rendering
- [ ] AI: «сделай экран логина» → apply → build → works
- [ ] AI: отменить генерацию → rollback
- [ ] Export → open in Android Studio → build with Gradle → works
- [ ] Thermal: запустить build при 80%+ battery temperature → no crash
- [ ] Low memory: open 5 apps → build → graceful error or success
- [ ] Build cancellation в каждой фазе → cleanup, без orphan-файлов
- [ ] TalkBack: navigate по всем экранам без потерь
- [ ] Landscape mode: все экраны корректно рендерятся
