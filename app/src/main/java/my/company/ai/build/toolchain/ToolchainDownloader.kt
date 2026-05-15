package my.company.ai.build.toolchain

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import my.company.ai.build.ToolchainConfig
import timber.log.Timber
import java.io.File
import java.net.URL
import java.security.MessageDigest

/**
 * Загрузчик toolchain бинарников с прогрессом и верификацией.
 */
class ToolchainDownloader(private val context: Context) {

    private val toolsDir = File(context.filesDir, "toolchain").apply { mkdirs() }

    data class DownloadProgress(
        val toolName: String,
        val downloadedBytes: Long,
        val totalBytes: Long,
    )

    /**
     * Проверяет SHA-256 файла.
     */
    fun verifySha256(file: File, expected: String): Boolean {
        if (expected == "TODO") return true
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { stream ->
            val buffer = ByteArray(8192)
            var read: Int
            while (stream.read(buffer).also { read = it } > 0) {
                digest.update(buffer, 0, read)
            }
        }
        val actual = digest.digest().joinToString("") { "%02x".format(it) }
        return actual.equals(expected, ignoreCase = true)
    }

    /**
     * Скачивает файл по URL в указанный путь с эмиссией прогресса.
     */
    fun downloadWithProgress(
        tool: ToolchainConfig.Tool,
        dest: File,
    ): Flow<DownloadProgress> = flow {
        val url = URL(tool.url)
        val connection = url.openConnection()
        val total = connection.contentLengthLong.takeIf { it > 0 } ?: tool.expectedSize

        dest.parentFile?.mkdirs()
        connection.getInputStream().use { input ->
            dest.outputStream().use { output ->
                val buffer = ByteArray(8192)
                var downloaded = 0L
                var read: Int
                while (input.read(buffer).also { read = it } > 0) {
                    output.write(buffer, 0, read)
                    downloaded += read
                    emit(DownloadProgress(tool.filename, downloaded, total))
                }
            }
        }
        if (tool.isExecutable) {
            dest.setExecutable(true)
        }
        Timber.d("Downloaded ${tool.filename}: ${dest.length()} bytes")
    }

    /**
     * Скачивает один инструмент (без прогресса).
     */
    suspend fun download(tool: ToolchainConfig.Tool): Result<File> = withContext(Dispatchers.IO) {
        try {
            val dest = File(toolsDir, tool.filename)
            Timber.d("Downloading ${tool.url} → ${dest.absolutePath}")
            URL(tool.url).openStream().use { input ->
                dest.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            if (tool.isExecutable) {
                dest.setExecutable(true)
            }
            Timber.d("Downloaded ${dest.length()} bytes")
            Result.success(dest)
        } catch (e: Exception) {
            Timber.e(e, "Download failed: ${tool.url}")
            Result.failure(e)
        }
    }

    /**
     * Проверяет, существует ли инструмент и верна ли его SHA-256.
     */
    fun isToolValid(tool: ToolchainConfig.Tool): Boolean {
        val file = File(toolsDir, tool.filename)
        if (!file.exists()) return false
        return verifySha256(file, tool.sha256)
    }

    /**
     * Возвращает файл инструмента (может не существовать).
     */
    fun getToolFile(filename: String): File = File(toolsDir, filename)
}
