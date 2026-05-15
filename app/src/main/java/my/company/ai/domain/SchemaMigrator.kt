package my.company.ai.domain

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import my.company.ai.data.json.AppJson
import my.company.ai.domain.model.ProjectJson

/**
 * Мигратор JSON-модели проекта между версиями schema.
 */
object SchemaMigrator {

    const val CURRENT_SCHEMA_VERSION = 1

    /**
     * Мигрирует проект к текущей версии схемы.
     */
    fun migrate(project: ProjectJson): ProjectJson {
        var current = project
        while (current.schemaVersion < CURRENT_SCHEMA_VERSION) {
            current = migrateOneStep(current)
        }
        return current
    }

    private fun migrateOneStep(project: ProjectJson): ProjectJson {
        return when (project.schemaVersion) {
            0 -> migrateV0ToV1(project)
            else -> project.copy(schemaVersion = CURRENT_SCHEMA_VERSION)
        }
    }

    /**
     * Миграция v0 → v1: добавляет schemaVersion, заполняет defaults.
     */
    private fun migrateV0ToV1(project: ProjectJson): ProjectJson {
        return project.copy(
            schemaVersion = 1,
            minSdk = project.minSdk.coerceAtLeast(29),
            targetSdk = project.targetSdk.coerceAtLeast(36),
        )
    }

    /**
     * Мигрирует сырой JSON-объект (для случаев, когда десериализация не работает).
     */
    fun migrateJsonObject(json: JsonObject): JsonObject {
        val version = json["schema_version"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
        if (version >= CURRENT_SCHEMA_VERSION) return json

        return buildJsonObject {
            json.entries.forEach { (key, value) -> put(key, value) }
            put("schema_version", JsonPrimitive(CURRENT_SCHEMA_VERSION))
        }
    }
}
