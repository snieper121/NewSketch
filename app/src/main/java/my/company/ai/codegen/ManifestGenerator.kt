package my.company.ai.codegen

import my.company.ai.domain.model.ProjectJson
import java.io.File

/**
 * Генерирует AndroidManifest.xml для проекта.
 * Требует minSdk из ToolchainConfig (по умолчанию 29).
 */
class ManifestGenerator(
    private val minSdk: Int = 29,
    private val targetSdk: Int = 36,
) : CodeGenerator {

    override fun generate(project: ProjectJson, outputDir: File): List<File> {
        val manifest = File(outputDir, "AndroidManifest.xml")
        val permissionsBlock = project.permissions.joinToString("\n") { perm ->
            "    <uses-permission android:name=\"$perm\" />"
        }

        val content = buildString {
            appendLine("<?xml version=\"1.0\" encoding=\"utf-8\"?>")
            appendLine("<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"")
            appendLine("    package=\"${project.packageName}\">")
            if (permissionsBlock.isNotBlank()) {
                appendLine()
                appendLine(permissionsBlock)
            }
            appendLine()
            appendLine("    <application")
            appendLine("        android:label=\"${project.name}\"")
            appendLine("        android:theme=\"@style/Theme.App\"")
            appendLine("        android:allowBackup=\"false\"")
            appendLine("        android:icon=\"@mipmap/ic_launcher\">")
            appendLine("        <activity android:name=\".MainActivity\"")
            appendLine("            android:exported=\"true\"")
            appendLine("            android:configChanges=\"orientation|screenSize|smallestScreenSize\">")
            appendLine("            <intent-filter>")
            appendLine("                <action android:name=\"android.intent.action.MAIN\" />")
            appendLine("                <category android:name=\"android.intent.category.LAUNCHER\" />")
            appendLine("            </intent-filter>")
            appendLine("        </activity>")
            appendLine("    </application>")
            appendLine("</manifest>")
        }

        manifest.parentFile?.mkdirs()
        manifest.writeText(content)
        return listOf(manifest)
    }
}
