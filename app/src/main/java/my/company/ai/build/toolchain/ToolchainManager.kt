package my.company.ai.build.toolchain

import android.content.Context
import java.io.File

/**
 * Управление toolchain: kotlinc, aapt2, d8, zipalign, apksigner.
 * M0: заглушка — бинарники будут bundled в assets позже.
 */
class ToolchainManager(private val context: Context) {

    val toolsDir: File = File(context.filesDir, "toolchain").apply { mkdirs() }

    fun kotlinCompiler(): File = File(toolsDir, "kotlinc/bin/kotlinc")
    fun aapt2(): File = File(toolsDir, "aapt2")
    fun d8(): File = File(toolsDir, "d8")
    fun zipalign(): File = File(toolsDir, "zipalign")
    fun apksigner(): File = File(toolsDir, "apksigner")

    /**
     * Проверяет наличие всех необходимых инструментов.
     */
    fun isToolchainReady(): Boolean {
        return listOf(kotlinCompiler(), aapt2(), d8(), zipalign(), apksigner())
            .all { it.exists() && it.canExecute() }
    }

    /**
     * Возвращает classpath с Android SDK stub'ами и Compose.
     * M0: возвращает пустой список — classpath будет сформирован позже.
     */
    fun resolveClasspath(): List<File> = emptyList()
}
