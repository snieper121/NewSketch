package my.company.ai.data.model

import kotlinx.serialization.Serializable

/**
 * JSON-модель проекта (source of truth).
 * Сохраняется как design/project.json.
 */
@Serializable
data class ProjectModel(
    val schemaVersion: Int = 2,
    val id: String,
    val name: String,
    val packageName: String,
    val minSdk: Int = 26,
    val targetSdk: Int = 35,
    val screens: List<String> = emptyList(),
    val launcherScreen: String = "main",
    val permissions: List<String> = listOf("INTERNET"),
    val dependencies: List<String> = emptyList(),
    val createdAt: String = "",
    val updatedAt: String = "",
)
