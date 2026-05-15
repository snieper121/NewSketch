package my.company.ai.build.toolchain

import android.content.Context
import my.company.ai.build.ToolchainConfig
import java.io.File

/**
 * Управление toolchain: пути, статус, classpath.
 */
class ToolchainManager(private val context: Context) {

    val toolsDir: File = File(context.filesDir, "toolchain").apply { mkdirs() }
    private val downloader = ToolchainDownloader(context)

    fun kotlinCompiler(): File = downloader.getToolFile("kotlinc.dex")
    fun aapt2(): File = downloader.getToolFile("aapt2")
    fun d8(): File = downloader.getToolFile("r8.dex")
    fun zipalign(): File = downloader.getToolFile("zipalign")
    fun apksigner(): File = downloader.getToolFile("apksigner.dex")
    fun androidJar(): File = downloader.getToolFile("android.jar")
    fun kotlinStdlib(): File = downloader.getToolFile("kotlin-stdlib.jar")

    /**
     * Проверяет наличие всех необходимых инструментов.
     */
    fun isToolchainReady(): Boolean {
        return ToolchainConfig.TOOLS.all { downloader.isToolValid(it) }
    }

    /**
     * Возвращает classpath для компиляции (android.jar + kotlin-stdlib + Compose).
     * M0: только android.jar и kotlin-stdlib — Compose classpath будет позже.
     */
    fun resolveClasspath(): List<File> {
        return ToolchainConfig.CLASSPATH_JARS.map { downloader.getToolFile(it) }
            .filter { it.exists() }
    }
}
