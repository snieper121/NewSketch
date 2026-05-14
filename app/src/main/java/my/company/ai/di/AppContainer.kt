package my.company.ai.di

import android.content.Context
import androidx.room.Room
import my.company.ai.data.local.AppDatabase
import my.company.ai.data.local.ProjectStorage
import my.company.ai.data.remote.AiService
import my.company.ai.data.remote.HttpClientFactory
import my.company.ai.data.remote.OpenAiCompatibleService
import my.company.ai.data.repository.AiRepository
import my.company.ai.data.repository.ProjectRepository
import my.company.ai.data.repository.SettingsRepository
import my.company.ai.data.template.KotlinProjectTemplate

/**
 * Ручной контейнер зависимостей (ADR-004).
 *
 * Все тяжёлые объекты ленивые: БД/HTTP-клиент создаются только по первому обращению.
 * Контейнер живёт столько же, сколько и процесс [my.company.ai.AiApp].
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    // ---- Local storage -----------------------------------------------------

    private val database: AppDatabase by lazy {
        Room.databaseBuilder(
            appContext,
            AppDatabase::class.java,
            "ai-sketch.db",
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    val projectStorage: ProjectStorage by lazy {
        ProjectStorage(appContext)
    }

    val projectTemplate: KotlinProjectTemplate by lazy {
        KotlinProjectTemplate(appContext)
    }

    // ---- Settings ----------------------------------------------------------

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(appContext)
    }

    // ---- Network / AI ------------------------------------------------------

    private val httpClient by lazy { HttpClientFactory.create() }

    private val aiService: AiService by lazy {
        OpenAiCompatibleService(httpClient, settingsRepository)
    }

    val aiRepository: AiRepository by lazy {
        AiRepository(aiService)
    }

    // ---- Repositories ------------------------------------------------------

    val projectRepository: ProjectRepository by lazy {
        ProjectRepository(
            projectDao = database.projectDao(),
            storage = projectStorage,
            template = projectTemplate,
        )
    }
}
