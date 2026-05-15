package my.company.ai.build.model

import kotlinx.serialization.json.Json
import java.io.File

/**
 * Результат выполнения фазы сборки.
 */
sealed class PhaseResult {
    data object Success : PhaseResult()
    data class Failure(val message: String, val cause: Throwable? = null) : PhaseResult()
}

/**
 * Состояние прогресса сборки — публикуется в UI.
 */
data class BuildProgress(
    val phaseName: String,
    val step: Int,
    val totalSteps: Int,
    val message: String,
)

/**
 * Контекст сборки — передаётся между фазами.
 */
class BuildContext(
    val projectDir: File,
    val buildDir: File,
    val outputApk: File,
    val packageName: String,
    val minSdk: Int = 29,
    val targetSdk: Int = 36,
    val json: Json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        isLenient = true
        namingStrategy = kotlinx.serialization.json.JsonNamingStrategy.SnakeCase
    },
) {
    val intermediateDir: File = File(buildDir, "intermediates").apply { mkdirs() }
    val classesDir: File = File(buildDir, "classes").apply { mkdirs() }
    val dexDir: File = File(buildDir, "dex").apply { mkdirs() }
    val apkUnsigned: File = File(buildDir, "app-unsigned.apk")
    val apkAligned: File = File(buildDir, "app-aligned.apk")
}
