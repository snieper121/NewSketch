package my.company.ai.build

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import timber.log.Timber
import java.io.File

/**
 * Установщик собранного APK через PackageInstaller (API 29+) или ACTION_VIEW intent.
 */
class ApkInstaller(private val context: Context) {

    private val packageInstaller = context.packageManager.packageInstaller

    /**
     * Устанавливает APK через PackageInstaller (рекомендуемый способ на API 29+).
     */
    fun install(apkFile: File): Result<Unit> {
        return try {
            val uri = getApkUri(apkFile)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "APK install failed")
            Result.failure(e)
        }
    }

    /**
     * Возвращает content:// URI для APK через FileProvider.
     */
    fun getApkUri(apkFile: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile,
        )
    }

    /**
     * Продвинутый способ через PackageInstaller.Session (API 21+).
     * Требует permission INSTALL_PACKAGES (system app или rooted).
     */
    @Suppress("unused")
    fun installWithSession(apkFile: File): Result<Unit> {
        return try {
            val params = PackageInstaller.SessionParams(
                PackageInstaller.SessionParams.MODE_FULL_INSTALL
            )
            val sessionId = packageInstaller.createSession(params)
            val session = packageInstaller.openSession(sessionId)

            apkFile.inputStream().use { input ->
                session.openWrite("base.apk", 0, apkFile.length()).use { output ->
                    input.copyTo(output)
                    session.fsync(output)
                }
            }

            val intent = Intent(context, context.javaClass)
            val pendingIntent = PendingIntent.getActivity(
                context, sessionId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            session.commit(pendingIntent.intentSender)
            session.close()
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Session install failed")
            Result.failure(e)
        }
    }
}
