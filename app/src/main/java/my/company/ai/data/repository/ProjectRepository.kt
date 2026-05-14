package my.company.ai.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import my.company.ai.data.local.ProjectDao
import my.company.ai.data.local.ProjectEntity
import my.company.ai.data.local.ProjectStorage
import my.company.ai.data.model.Project
import my.company.ai.data.model.ProjectFile
import my.company.ai.data.template.KotlinProjectTemplate
import java.util.UUID

/**
 * Главный фасад над проектами: создать/удалить/переименовать, получить дерево
 * файлов, читать/записывать содержимое.
 *
 * Сочетает:
 *  * [ProjectDao] — индекс метаданных (для быстрого списка);
 *  * [ProjectStorage] — файловое хранилище (исходники);
 *  * [KotlinProjectTemplate] — генерация Gradle/Compose-шаблона.
 */
class ProjectRepository(
    private val projectDao: ProjectDao,
    private val storage: ProjectStorage,
    private val template: KotlinProjectTemplate,
) {

    fun observeProjects(): Flow<List<Project>> =
        projectDao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getProject(id: String): Project? =
        projectDao.findById(id)?.toDomain()

    /**
     * Создаёт новый проект: генерирует id, разворачивает шаблон, сохраняет
     * запись в Room. При ошибке кидает [IllegalArgumentException] — вызывающий
     * показывает сообщение пользователю.
     */
    suspend fun createProject(name: String, packageName: String): Project {
        require(name.isNotBlank()) { "Имя проекта не должно быть пустым" }
        require(Project.isValidPackageName(packageName)) {
            "Некорректный package name: $packageName"
        }

        val id = UUID.randomUUID().toString()
        val dir = storage.createProjectDir(id)
        val now = System.currentTimeMillis()

        template.materialize(
            targetDir = dir,
            projectName = name,
            packageName = packageName,
        )

        val entity = ProjectEntity(
            id = id,
            name = name,
            packageName = packageName,
            rootPath = dir.absolutePath,
            createdAt = now,
            updatedAt = now,
        )
        projectDao.upsert(entity)
        return entity.toDomain()
    }

    suspend fun deleteProject(id: String) {
        storage.deleteProject(id)
        projectDao.deleteById(id)
    }

    suspend fun fileTree(projectId: String): ProjectFile = storage.tree(projectId)

    suspend fun readFile(projectId: String, relativePath: String): String =
        storage.readText(projectId, relativePath)

    suspend fun writeFile(projectId: String, relativePath: String, content: String) {
        storage.writeText(projectId, relativePath, content)
        projectDao.touch(projectId, System.currentTimeMillis())
    }
}
