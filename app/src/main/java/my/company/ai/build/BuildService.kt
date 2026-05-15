package my.company.ai.build

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import my.company.ai.MainActivity
import my.company.ai.R
import my.company.ai.build.model.BuildContext
import my.company.ai.build.model.BuildProgress
import my.company.ai.build.model.PhaseResult
import my.company.ai.build.model.SampleProject
import java.io.File

/**
 * Foreground service для длительной сборки APK.
 */
class BuildService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var buildJob: Job? = null
    private val binder = LocalBinder()

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val projectId = intent?.getStringExtra(EXTRA_PROJECT_ID) ?: ""
        if (projectId.isNotBlank()) {
            startBuild(projectId)
        }
        return START_NOT_STICKY
    }

    fun startBuild(projectId: String) {
        if (buildJob?.isActive == true) return

        val buildDir = File(cacheDir, "ai_ide_build_$projectId").apply { mkdirs() }
        val projectDir = File(buildDir, "project").apply { mkdirs() }
        val outputApk = File(buildDir, "app.apk")

        val context = BuildContext(
            projectDir = projectDir,
            buildDir = buildDir,
            outputApk = outputApk,
            packageName = SampleProject.PACKAGE,
            minSdk = SampleProject.MIN_SDK,
            targetSdk = SampleProject.TARGET_SDK,
        )

        val app = applicationContext as my.company.ai.AiApp
        val pipeline = app.container.buildPipeline

        startForeground(NOTIF_ID, buildNotification("Сборка...", 0, 0, true))

        buildJob = serviceScope.launch {
            launch {
                pipeline.logs.collect { log ->
                    updateNotification("Сборка", log, indeterminate = true)
                }
            }

            launch {
                pipeline.progress.collect { progress: BuildProgress ->
                    val percent = if (progress.totalSteps > 0) {
                        progress.step * 100 / progress.totalSteps
                    } else 0
                    updateNotification(
                        "Сборка: ${progress.phaseName}",
                        "${progress.step}/${progress.totalSteps} — ${progress.message}",
                        percent,
                    )
                }
            }

            pipeline.run(context)

            pipeline.result.collect { result ->
                when (result) {
                    is PhaseResult.Success -> {
                        updateNotification(
                            "Сборка завершена",
                            "APK готов: ${outputApk.absolutePath}",
                            100,
                        )
                        stopForeground(STOP_FOREGROUND_DETACH)
                    }
                    is PhaseResult.Failure -> {
                        updateNotification(
                            "Сборка не удалась",
                            result.message,
                            0,
                            isError = true,
                        )
                        stopForeground(STOP_FOREGROUND_DETACH)
                    }
                }
            }
        }
    }

    fun cancelBuild() {
        buildJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Сборка APK", NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Уведомления о процессе сборки"
            }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(
        title: String,
        percent: Int,
        max: Int = 100,
        indeterminate: Boolean = false,
    ): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setProgress(max, percent, indeterminate)
            .build()
    }

    private fun updateNotification(
        title: String,
        text: String,
        percent: Int = 0,
        indeterminate: Boolean = false,
        isError: Boolean = false,
    ) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(!isError)
            .setProgress(100, percent, indeterminate)
            .build()

        getSystemService(NotificationManager::class.java)?.notify(NOTIF_ID, notification)
    }

    inner class LocalBinder : Binder() {
        fun getService(): BuildService = this@BuildService
    }

    companion object {
        private const val CHANNEL_ID = "build_channel"
        private const val NOTIF_ID = 1001
        const val EXTRA_PROJECT_ID = "project_id"
    }
}
