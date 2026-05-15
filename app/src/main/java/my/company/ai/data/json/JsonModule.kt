package my.company.ai.data.json

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy

/**
 * Единый экземпляр Json для всего приложения.
 */
@OptIn(ExperimentalSerializationApi::class)
val AppJson = Json {
    prettyPrint = true
    prettyPrintIndent = "  "
    ignoreUnknownKeys = true
    encodeDefaults = true
    isLenient = true
    namingStrategy = JsonNamingStrategy.SnakeCase
}
