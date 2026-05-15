package my.company.ai.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import my.company.ai.data.local.ProjectStorage
import my.company.ai.domain.model.ProjectJson
import my.company.ai.domain.model.ScreenJson
import my.company.ai.domain.model.StateJson
import java.util.UUID

/**
 * Репозиторий для JSON-моделей проекта.
 *
 * Читает/пишет `design/project.json` и предоставляет CRUD
 * для экранов, состояния, виджетов, ресурсов.
 */
class ProjectModelRepository(
    private val storage: ProjectStorage,
) {

    companion object {
        private const val PROJECT_JSON = "design/project.json"
    }

    suspend fun getProject(projectId: String): ProjectJson? {
        return storage.readJson<ProjectJson>(projectId, PROJECT_JSON)
    }

    suspend fun saveProject(projectId: String, project: ProjectJson) {
        storage.writeJson(projectId, PROJECT_JSON, project)
    }

    /**
     * Создаёт новый проект с минимальной структурой.
     */
    suspend fun createProject(
        projectId: String,
        name: String,
        packageName: String,
        initialScreen: ScreenJson = defaultMainScreen(),
    ): ProjectJson = withContext(Dispatchers.IO) {
        storage.createDesignDir(projectId)
        val project = ProjectJson(
            name = name,
            packageName = packageName,
            screens = listOf(initialScreen),
        )
        saveProject(projectId, project)
        project
    }

    // --- Screen CRUD ---

    suspend fun addScreen(projectId: String, screen: ScreenJson): Result<ProjectJson> =
        updateProject(projectId) { project ->
            if (project.screens.any { it.id == screen.id }) {
                return@updateProject Result.failure(IllegalArgumentException("Экран с id=${screen.id} уже существует"))
            }
            Result.success(project.copy(screens = project.screens + screen))
        }

    suspend fun removeScreen(projectId: String, screenId: String): Result<ProjectJson> =
        updateProject(projectId) { project ->
            val updated = project.screens.filterNot { it.id == screenId }
            if (updated.size == project.screens.size) {
                return@updateProject Result.failure(IllegalArgumentException("Экран $screenId не найден"))
            }
            Result.success(project.copy(screens = updated))
        }

    suspend fun updateScreen(projectId: String, screen: ScreenJson): Result<ProjectJson> =
        updateProject(projectId) { project ->
            val index = project.screens.indexOfFirst { it.id == screen.id }
            if (index == -1) {
                return@updateProject Result.failure(IllegalArgumentException("Экран ${screen.id} не найден"))
            }
            val updated = project.screens.toMutableList().apply { set(index, screen) }
            Result.success(project.copy(screens = updated))
        }

    // --- State CRUD ---

    suspend fun addState(projectId: String, state: StateJson): Result<ProjectJson> =
        updateProject(projectId) { project ->
            if (project.state.any { it.id == state.id }) {
                return@updateProject Result.failure(IllegalArgumentException("Состояние с id=${state.id} уже существует"))
            }
            Result.success(project.copy(state = project.state + state))
        }

    suspend fun removeState(projectId: String, stateId: String): Result<ProjectJson> =
        updateProject(projectId) { project ->
            val updated = project.state.filterNot { it.id == stateId }
            Result.success(project.copy(state = updated))
        }

    // --- Private helpers ---

    private suspend inline fun updateProject(
        projectId: String,
        crossinline block: (ProjectJson) -> Result<ProjectJson>,
    ): Result<ProjectJson> = withContext(Dispatchers.IO) {
        val current = getProject(projectId)
            ?: return@withContext Result.failure(IllegalStateException("Проект $projectId не найден"))
        block(current).onSuccess { saveProject(projectId, it) }
    }
}

private fun defaultMainScreen(): ScreenJson = ScreenJson(
    id = UUID.randomUUID().toString(),
    name = "Главный экран",
    route = "main",
    isStart = true,
    widgets = emptyList(),
)
