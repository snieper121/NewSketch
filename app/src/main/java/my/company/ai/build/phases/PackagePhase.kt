package my.company.ai.build.phases

import my.company.ai.build.BuildPhase
import my.company.ai.build.model.BuildContext
import my.company.ai.build.model.PhaseResult
import my.company.ai.build.toolchain.ToolchainManager
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Фаза 7–9: merge APK → zipalign → sign.
 *
 * M0: создаёт минимальный APK (unsigned) из resources.arsc и classes.dex.
 * Без реального zipalign/apksigner — toolchain ещё не загружен.
 */
class PackagePhase(
    private val toolchain: ToolchainManager,
) : BuildPhase {
    override val name: String = "Package & Sign"

    override suspend fun execute(context: BuildContext): PhaseResult {
        return try {
            val unsignedApk = File(context.buildDir, "unsigned.apk")
            ZipOutputStream(FileOutputStream(unsignedApk)).use { zos ->
                // M0: пустой classes.dex placeholder (минимальный DEX заголовок)
                zos.putNextEntry(ZipEntry("classes.dex"))
                zos.write(MINIMAL_DEX_HEADER)
                zos.closeEntry()

                // AndroidManifest.xml placeholder
                zos.putNextEntry(ZipEntry("AndroidManifest.xml"))
                zos.closeEntry()

                // resources.arsc placeholder
                zos.putNextEntry(ZipEntry("resources.arsc"))
                zos.closeEntry()
            }

            // Копируем как final APK
            unsignedApk.copyTo(context.outputApk, overwrite = true)

            PhaseResult.Success
        } catch (e: Exception) {
            PhaseResult.Failure("Ошибка упаковки APK: ${e.message}", e)
        }
    }

    companion object {
        // Минимальный валидный DEX заголовок (035\n + null + checksum + signature + file_size)
        private val MINIMAL_DEX_HEADER = byteArrayOf(
            0x64, 0x65, 0x78, 0x0a, 0x30, 0x33, 0x35, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x20, 0x00, 0x00, 0x00
        )
    }
}
