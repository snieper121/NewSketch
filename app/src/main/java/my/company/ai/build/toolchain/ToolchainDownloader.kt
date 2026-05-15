package my.company.ai.build.toolchain

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.net.URL
import java.security.MessageDigest

/**
 * Загрузчик toolchain бинарников.
 *
 * M0: заглушка. Реальная загрузка будет реализована после определения
 * источников бинарников (kotlinc, aapt2, d8, zipalign, apksigner).
 */
class ToolchainDownloader(private val context: Context) {

    private val toolsDir = File(context.filesDir, "toolchain").apply { mkdirs() }

    data class ToolSpec(
        val name: String,
        val url: String,
        val sha256: String,
        val executableName: String,
    )

    /**
     * Проверяет SHA-256 файла.
     */
    fun verifySha256(file: File, expected: String): Boolean {
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
     * Скачивает файл по URL в указанный путь.
     */
    suspend fun download(url: String, dest: File): Result<File> = withContext(Dispatchers.IO) {
        try {
            Timber.d("Downloading $url → ${dest.absolutePath}")
            dest.parentFile?.mkdirs()
            URL(url).openStream().use { input ->
                dest.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            dest.setExecutable(true)
            Timber.d("Downloaded ${dest.length()} bytes")
            Result.success(dest)
        } catch (e: Exception) {
            Timber.e(e, "Download failed: $url")
            Result.failure(e)
        }
    }

    /**
     * Проверяет, существует ли бинарник и верна ли его SHA-256.
     */
    fun isToolValid(spec: ToolSpec): Boolean {
        val file = File(toolsDir, spec.executableName)
        if (!file.exists()) return false
        return verifySha256(file, spec.sha256)
    }

    companion object {
        // TODO: добавить реальные URL и SHA-256 для kotlinc, aapt2, d8, zipalign, apksigner
        val M0_TOOLS: List<ToolSpec> = emptyList()
    }
}
