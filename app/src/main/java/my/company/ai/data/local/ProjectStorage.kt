package my.company.ai.data.local

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import my.company.ai.data.model.ProjectFile
import java.io.File

/**
 * Хранилище исходников пользовательских проектов на файловой системе.
 *
 * Базовая директория: `context.filesDir/projects/<projectId>/`.
 * Всё IO выполняется на [Dispatchers.IO], чтобы не блокировать корутины UI.
 */
class ProjectStorage(context: Context) {

    private val root: File = File(context.filesDir, "projects").apply { mkdirs() }

    fun projectDir(projectId: String): File = File(root, projectId)

    suspend fun createProjectDir(projectId: String): File = withContext(Dispatchers.IO) {
        projectDir(projectId).apply { mkdirs() }
    }

    suspend fun deleteProject(projectId: String): Boolean = withContext(Dispatchers.IO) {
        projectDir(projectId).deleteRecursively()
    }

    /** Строит дерево файлов проекта. Скрывает `build/` и другие служебные каталоги. */
    suspend fun tree(projectId: String): ProjectFile = withContext(Dispatchers.IO) {
        val dir = projectDir(projectId)
        require(dir.exists()) { "Project directory not found: $dir" }
        buildNode(dir, dir)
    }

    suspend fun readText(projectId: String, relativePath: String): String =
        withContext(Dispatchers.IO) {
            File(projectDir(projectId), relativePath).readText()
        }

    suspend fun writeText(projectId: String, relativePath: String, content: String) =
        withContext(Dispatchers.IO) {
            val file = File(projectDir(projectId), relativePath)
            file.parentFile?.mkdirs()
            file.writeText(content)
        }

    /**
     * Читает и парсит JSON-файл проекта.
     */
    suspend inline fun <reified T> readJson(projectId: String, relativePath: String): T? =
        withContext(Dispatchers.IO) {
            try {
                val file = File(projectDir(projectId), relativePath)
                if (!file.exists()) return@withContext null
                my.company.ai.data.json.AppJson.decodeFromString<T>(file.readText())
            } catch (e: Exception) {
                null
            }
        }

    /**
     * Сериализует объект в JSON-файл проекта.
     */
    suspend inline fun <reified T> writeJson(
        projectId: String,
        relativePath: String,
        value: T,
    ) = withContext(Dispatchers.IO) {
        val file = File(projectDir(projectId), relativePath)
        file.parentFile?.mkdirs()
        file.writeText(my.company.ai.data.json.AppJson.encodeToString(value))
    }

    /**
     * Создаёт директорию design/ для JSON-моделей проекта.
     */
    suspend fun createDesignDir(projectId: String): File = withContext(Dispatchers.IO) {
        File(projectDir(projectId), "design").apply { mkdirs() }
    }

    private fun buildNode(root: File, file: File): ProjectFile {
        val rel = file.relativeTo(root).invariantSeparatorsPath
        if (!file.isDirectory) {
            return ProjectFile(relativePath = rel, isDirectory = false)
        }
        val children = file.listFiles()
            ?.asSequence()
            ?.filterNot { it.name in SKIP_DIRS }
            ?.sortedWith(compareByDescending<File> { it.isDirectory }.thenBy { it.name.lowercase() })
            ?.map { buildNode(root, it) }
            ?.toList()
            ?: emptyList()
        return ProjectFile(relativePath = rel, isDirectory = true, children = children)
    }

    private companion object {
        val SKIP_DIRS = setOf(".gradle", "build", ".idea", ".kotlin", "local.properties")
    }
}
