# 07 — AI Integration

## Обзор

AI-ассистент помогает пользователю генерировать экраны, виджеты и логику через чат. AI работает на уровне JSON-модели (не сырого кода) — это даёт структурированные, предсказуемые результаты.

## Архитектура

```
User message
  → ContextBuilder (собирает контекст проекта)
  → SecretFilter (удаляет API keys, passwords из контекста)
  → TokenCounter (оценивает cost перед отправкой)
  → PromptBuilder (system prompt + context + user message)
  → PromptCache (проверяет кэш)
  → AiProvider (HTTP call) / streaming
  → ResponseParser (structured output → JSON patches)
  → DiffEngine (применяет изменения к модели)
  → UI: DiffViewer → Apply / Reject
  → UsageTracker (записывает cost)
```

## Provider Abstraction

```kotlin
interface AiProvider {
    val id: String
    val name: String
    val supportsStreaming: Boolean
    val supportsToolUse: Boolean

    suspend fun chat(messages: List<ChatMessage>, tools: List<Tool>): AiResponse
    fun chatStream(messages: List<ChatMessage>, tools: List<Tool>): Flow<AiChunk>

    /** Количество токенов для данной модели. */
    fun countTokens(text: String): Int

    /** Стоимость в USD для input/output токенов. */
    fun pricing(): Pricing
}

data class Pricing(
    val inputPerMTokens: Double,  // USD per 1M input tokens
    val outputPerMTokens: Double  // USD per 1M output tokens
)

class OpenAiProvider(private val config: AiConfig) : AiProvider { ... }
class ClaudeProvider(private val config: AiConfig) : AiProvider { ... }
class CustomProvider(private val config: AiConfig) : AiProvider { ... }

data class AiConfig(
    val apiKey: String,
    val baseUrl: String,
    val model: String,
    val maxTokens: Int = 4096,
    val temperature: Double = 0.3
)
```

Поддерживаемые провайдеры:
- **OpenAI** (GPT-4o, GPT-4o-mini, GPT-4-turbo)
- **Anthropic Claude** (Claude 3.5 Sonnet, Claude 3.5 Haiku)
- **OpenAI-compatible** (Ollama, LM Studio, OpenRouter, DeepSeek)
- **Local LLM** (через OpenAI-compatible Ollama) — v1.0+

## Context Builder

AI должен понимать текущее состояние проекта. Контекст собирается автоматически:

```kotlin
class ContextBuilder(
    private val project: ProjectModel,
    private val tokenBudget: Int = 4000
) {
    fun build(currentScreenId: String?): String = buildString {
        appendLine("## Project: ${project.name}")
        appendLine("Package: ${project.packageName}")
        appendLine("Screens: ${project.screens.joinToString()}")
        appendLine("Min SDK: ${project.minSdk}")
        appendLine()

        // Текущий экран (полный JSON)
        currentScreenId?.let { id ->
            val screen = loadScreen(id)
            appendLine("## Current Screen: ${screen.name}")
            appendLine("```json")
            appendLine(Json.encodeToString(screen))
            appendLine("```")
        }

        // Список виджетов и state всех экранов (краткий)
        appendLine("## All Screens Summary:")
        project.screens.forEach { screenId ->
            val s = loadScreen(screenId)
            appendLine("- ${s.name}: ${s.state.size} vars, ${countWidgets(s.root)} widgets")
        }

        // Доступные ресурсы
        appendLine("## Resources:")
        appendLine("Colors: ${colors.keys.joinToString()}")
        appendLine("Strings: ${strings.keys.joinToString()}")
    }
}
```

**Стратегии для большого проекта** (если контекст > tokenBudget):
1. **Full current screen** + summary остальных (всегда)
2. Если всё ещё > budget → убираем resources summary
3. Если всё ещё > budget → сокращаем current screen до root structure
4. Edge case: уведомляем пользователя «проект слишком большой, уточните запрос»

## Secret Filter

Перед отправкой в AI — чистим от секретов.

```kotlin
class SecretFilter {
    private val patterns = listOf(
        Regex("(?i)api[_-]?key\\s*[=:]\\s*[\"']?([a-zA-Z0-9_-]{20,})"),
        Regex("sk-[a-zA-Z0-9]{20,}"),                    // OpenAI
        Regex("sk-ant-[a-zA-Z0-9-]{80,}"),              // Anthropic
        Regex("AIza[0-9A-Za-z_-]{35}"),                  // Google
        Regex("ghp_[a-zA-Z0-9]{36}"),                   // GitHub
        Regex("(?i)password\\s*[=:]\\s*[\"']?([^\\s\"']+)"),
        Regex("(?i)secret\\s*[=:]\\s*[\"']?([^\\s\"']+)"),
        Regex("-----BEGIN (RSA |EC )?PRIVATE KEY-----[\\s\\S]+?-----END"),
    )

    fun filter(text: String): FilterResult {
        val findings = mutableListOf<String>()
        var filtered = text
        patterns.forEach { pattern ->
            filtered = pattern.replace(filtered) { match ->
                findings += match.value.take(30) + "..."
                "[REDACTED]"
            }
        }
        return FilterResult(filtered, findings)
    }

    data class FilterResult(val text: String, val redactions: List<String>)
}
```

Если найдены секреты:
- UI показывает warning: «Обнаружены секреты, они заменены на [REDACTED]»
- Пользователь подтверждает отправку или отменяет

## Token Counter

Показываем стоимость **до** отправки.

```kotlin
class TokenCounter(private val provider: AiProvider) {
    fun estimate(prompt: String): Estimate {
        val inputTokens = provider.countTokens(prompt)
        val expectedOutput = 1000 // consertive estimate
        val pricing = provider.pricing()
        val cost = (inputTokens * pricing.inputPerMTokens +
                    expectedOutput * pricing.outputPerMTokens) / 1_000_000
        return Estimate(inputTokens, expectedOutput, cost)
    }

    data class Estimate(
        val inputTokens: Int,
        val estimatedOutputTokens: Int,
        val estimatedCostUsd: Double
    )
}
```

**UI:** в ChatScreen перед отправкой — плашка:
> Запрос: ~3200 токенов, примерная стоимость $0.005

Можно отключить в настройках (по умолчанию показывается).

**Tokenizers:**
- OpenAI: tiktoken (есть Java/Kotlin port)
- Claude: приблизительная оценка (4 chars = 1 token) — Anthropic не публикует точный tokenizer
- Local: провайдер-specific

## Prompt Templates

Разные задачи → разные промпты.

```kotlin
object PromptTemplates {
    const val CREATE_SCREEN = """
Create a new screen for the following request:
{{request}}

Output a JSON patch of type "add_screen" with the new screen definition.
Use only supported widget types: {{widgets}}.
"""

    const val MODIFY_SCREEN = """
Modify the current screen for the following request:
{{request}}

Current screen state:
{{screen_json}}

Output JSON patches to implement the change.
"""

    const val FIX_ERROR = """
The user is getting this build error:
{{error}}

Project context:
{{context}}

Output JSON patches to fix the error, or explain if it cannot be fixed automatically.
"""

    const val EXPLAIN_CODE = """
Explain the following screen/widget in simple terms (in {{language}}):
{{code}}

No patches needed — just plain text explanation.
"""

    const val REFACTOR = """
Refactor the current screen for better structure/readability:
{{screen_json}}

Output JSON patches that preserve behavior but improve code quality.
"""
}
```

UI предоставляет быстрые действия:
- «Создать экран» → `CREATE_SCREEN`
- «Исправить ошибку» → `FIX_ERROR` (автоматически при build failure)
- «Объяснить» → `EXPLAIN_CODE`
- Free chat → general prompt

## System Prompt (patch-based)

```
You are an AI assistant for a mobile IDE that builds Android Compose apps.
You modify the app by outputting JSON patches to the project model.

Rules:
- Output ONLY valid JSON in the specified format
- Use only supported widget types: Text, Button, TextField, Column, Row, Card, Scaffold, TopAppBar, Icon, Image, Spacer, Divider, Switch, LazyColumn, LazyRow
- Use only supported action types: setState, navigate, navigateBack, showSnackbar, showDialog, toggleState, incrementState
- Do NOT use action type "custom" — it is forbidden
- Expressions must be simple (arithmetic, comparisons, state refs) — no function calls
- Generate unique IDs for new widgets (format: type_randomHex4)
- Keep state variable names camelCase
- Reference existing screens for navigation
- Include explanation of what you did

Output format:
{
  "explanation": "What I did and why",
  "patches": [
    { "op": "add_screen", "screen": { ... } },
    { "op": "replace_root", "screenId": "main", "root": { ... } },
    { "op": "add_state", "screenId": "main", "state": { "name": "x", "type": "Int", "initial": "0" } },
    { "op": "add_event", "screenId": "main", "event": { ... } },
    { "op": "update_widget", "screenId": "main", "widgetId": "btn_1", "properties": { ... } },
    { "op": "add_child", "screenId": "main", "parentId": "col_1", "index": 2, "widget": { ... } },
    { "op": "remove_widget", "screenId": "main", "widgetId": "text_3" }
  ]
}
```

## Tool Use / Function Calling

Для провайдеров с поддержкой tool use (OpenAI, Claude):

```kotlin
val tools = listOf(
    Tool(
        name = "modify_project",
        description = "Apply changes to the project model",
        parameters = patchesSchema
    )
)
```

Для провайдеров без tool use — парсим JSON из текстового ответа (regex extraction между ````json` блоками).

## Response Parser

```kotlin
class ResponseParser {
    fun parse(response: String): AiResult {
        // Попытка 1: tool_call result
        // Попытка 2: JSON в markdown code block
        // Попытка 3: raw JSON в начале/конце
        // Fallback: текстовый ответ без patches
        val json = extractJson(response) ?: return AiResult.TextOnly(response)
        return try {
            val patches = Json.decodeFromString<PatchResponse>(json)
            AiResult.Patches(patches.explanation, patches.patches)
        } catch (e: SerializationException) {
            AiResult.Error("Не удалось разобрать ответ AI: ${e.message}")
        }
    }
}

sealed class AiResult {
    data class Patches(val explanation: String, val patches: List<Patch>) : AiResult()
    data class TextOnly(val text: String) : AiResult()
    data class Error(val message: String) : AiResult()
}
```

## Diff Application

Patches применяются к JSON-модели атомарно:

```kotlin
class DiffEngine {
    fun apply(project: ProjectModel, patches: List<Patch>): ProjectModel {
        var current = project
        for (patch in patches) {
            current = when (patch) {
                is AddScreen -> current.copy(screens = current.screens + patch.screen.id)
                is ReplaceRoot -> current.updateScreen(patch.screenId) { it.copy(root = patch.root) }
                is AddState -> current.updateScreen(patch.screenId) { it.addState(patch.state) }
                is AddChild -> current.updateScreen(patch.screenId) { it.insertChild(patch.parentId, patch.index, patch.widget) }
                is UpdateWidget -> current.updateScreen(patch.screenId) { it.updateWidget(patch.widgetId, patch.properties) }
                is RemoveWidget -> current.updateScreen(patch.screenId) { it.removeWidget(patch.widgetId) }
                // ...
            }
        }
        return current
    }
}
```

**Rollback:** перед apply сохраняется snapshot. Reject → восстановление snapshot.

**Validation перед apply:** каждый patch валидируется (id refs, schema). Невалидные patches → skip + warning.

## UI: Diff Viewer

Пользователь видит:
1. **Explanation** — что AI сделал (текст)
2. **Estimated cost** — сколько токенов потрачено (после ответа)
3. **Visual diff** — добавленные виджеты зелёным, удалённые красным, изменённые жёлтым
4. **Код preview** (toggle) — Kotlin что будет сгенерирован
5. **Кнопки:** [Применить] [Отменить] [Применить частично]

«Применить частично» открывает список патчей с галочками — можно принять только нужные.

## Streaming

Для UX — streaming ответа (текст появляется по мере генерации):

```kotlin
aiProvider.chatStream(messages, tools).collect { chunk ->
    _uiState.update { it.copy(streamingText = it.streamingText + chunk.text) }
}
// После завершения — парсим полный ответ
```

Для не-streaming провайдеров (custom endpoints без SSE) — показываем spinner.

## Response Cache

Идентичные промпты → кэш. Экономит $$$ и время.

```kotlin
class PromptCache(private val maxEntries: Int = 100) {
    private val cache = LinkedHashMap<String, CacheEntry>(maxEntries, 0.75f, true)

    fun get(prompt: String, model: String): CacheEntry? {
        val key = "$model:${sha256(prompt)}"
        return cache[key]
    }

    fun put(prompt: String, model: String, result: AiResult, cost: Double) {
        val key = "$model:${sha256(prompt)}"
        cache[key] = CacheEntry(result, System.currentTimeMillis(), cost)
        if (cache.size > maxEntries) cache.remove(cache.keys.first())
    }

    data class CacheEntry(val result: AiResult, val timestamp: Long, val originalCost: Double)
}
```

TTL: 1 час (при повторном запросе через час — refetch).

## Cost Tracking

```kotlin
class UsageTracker(private val storage: UsageStorage) {
    data class Usage(
        val date: LocalDate,
        val inputTokens: Long,
        val outputTokens: Long,
        val costUsd: Double,
        val requestsCount: Int
    )

    suspend fun record(inputTokens: Int, outputTokens: Int, costUsd: Double) { ... }
    fun observeDaily(): Flow<List<Usage>> { ... }
    suspend fun total(from: LocalDate, to: LocalDate): Usage { ... }
}
```

Показывается в Settings → AI → Usage:
- График за последние 30 дней
- Total cost
- Breakdown по моделям
- Budget alerts (user-set)

## Error Handling

| Ситуация | Действие |
|----------|----------|
| Network error | Retry 1 раз с exp backoff, затем UiEvent.Error |
| Invalid JSON от AI | Retry с уточняющим промптом: «Your previous response was not valid JSON» |
| Rate limit (429) | Exponential backoff (2s, 4s, 8s, 16s), max 3 retries |
| API key invalid (401) | UiEvent.Error → открыть Settings с подсветкой поля |
| Quota exceeded (402/403) | UiEvent.Error → «Недостаточно средств на балансе» |
| Timeout (>30s default) | Cancel, предложить повторить |
| Patch references несуществующий widget | Skip patch, show warning в Explanation |
| Model unavailable (503) | Retry 1 раз, затем fallback на другую модель (если настроено) |

## Model Recommendations (UX)

При первой настройке — рекомендации:

```
Для быстрых изменений (чат, простые экраны):
  • OpenAI: gpt-4o-mini (быстро, дёшево)
  • Anthropic: claude-3-5-haiku (дёшево)

Для сложных задач (многоэкранные, рефакторинг):
  • OpenAI: gpt-4o (точнее)
  • Anthropic: claude-3-5-sonnet (лучший code quality)

Для конфиденциальности:
  • Ollama + llama3.1-70b (локально, бесплатно)
  • Ollama + qwen2.5-coder (оптимизирован для кода)
```

## Security

- API key → `EncryptedSharedPreferences` (v0.2+)
- Ключ НЕ передаётся в: логи, crash reports, analytics, UI snapshots
- Контекст фильтруется `SecretFilter` перед отправкой
- Пользователь явно подтверждает отправку при первом использовании
- Local LLM — данные не покидают устройство
- Кеш очищается при logout / смене API key
- Network traffic только HTTPS, certificate pinning для known providers (опционально)
