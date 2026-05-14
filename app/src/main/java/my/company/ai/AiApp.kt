package my.company.ai

import android.app.Application
import my.company.ai.di.AppContainer
import timber.log.Timber

/**
 * Главный Application-класс. Точка инициализации:
 *  - логирование (Timber) в debug;
 *  - контейнер зависимостей [AppContainer], из которого читают ViewModel'и.
 *
 * Мы сознательно избегаем Hilt/Koin на этапе каркаса — см. ADR-004.
 */
class AiApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        container = AppContainer(this)
    }
}
