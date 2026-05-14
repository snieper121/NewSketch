package my.company.ai.data.template

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Генерирует Compose-проект из шаблона.
 * Создаёт: Gradle files, MainActivity (Compose), Theme, NavHost, MainScreen.
 */
class KotlinProjectTemplate(@Suppress("unused") private val context: Context) {

    suspend fun materialize(targetDir: File, projectName: String, packageName: String) =
        withContext(Dispatchers.IO) {
            targetDir.mkdirs()
            val packagePath = packageName.replace('.', '/')
            val vars = mapOf(
                "projectName" to projectName,
                "packageName" to packageName,
                "packagePath" to packagePath,
            )

            TEMPLATE_FILES.forEach { (pathTemplate, contentTemplate) ->
                val relPath = pathTemplate.replace("_pkg_", packagePath)
                val file = File(targetDir, relPath)
                file.parentFile?.mkdirs()
                file.writeText(contentTemplate.render(vars))
            }
        }

    private fun String.render(vars: Map<String, String>): String =
        vars.entries.fold(this) { acc, (k, v) -> acc.replace("{{$k}}", v) }

    private companion object {
        val TEMPLATE_FILES: List<Pair<String, String>> = listOf(
            "settings.gradle.kts" to SETTINGS_GRADLE,
            "build.gradle.kts" to ROOT_BUILD_GRADLE,
            "gradle.properties" to GRADLE_PROPERTIES,
            "app/build.gradle.kts" to APP_BUILD_GRADLE,
            "app/src/main/AndroidManifest.xml" to MANIFEST,
            "app/src/main/java/_pkg_/MainActivity.kt" to MAIN_ACTIVITY,
            "app/src/main/java/_pkg_/ui/theme/Theme.kt" to THEME_KT,
            "app/src/main/java/_pkg_/ui/theme/Color.kt" to COLOR_KT,
            "app/src/main/java/_pkg_/screens/MainScreen.kt" to MAIN_SCREEN,
            "app/src/main/res/values/strings.xml" to STRINGS_XML,
            "app/src/main/res/values/themes.xml" to THEMES_XML,
            "README.md" to README,
        )
    }
}

private const val SETTINGS_GRADLE = """pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "{{projectName}}"
include(":app")
"""

private const val ROOT_BUILD_GRADLE = """plugins {
    id("com.android.application") version "8.10.0" apply false
    id("org.jetbrains.kotlin.android") version "2.2.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.0" apply false
}
"""

private const val GRADLE_PROPERTIES = """org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
android.nonTransitiveRClass=true
kotlin.code.style=official
"""

private const val APP_BUILD_GRADLE = """plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "{{packageName}}"
    compileSdk = 35

    defaultConfig {
        applicationId = "{{packageName}}"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
"""

private const val MANIFEST = """<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application
        android:allowBackup="true"
        android:label="{{projectName}}"
        android:theme="@style/Theme.App">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.App">
            <intent-filter>
                <action android:name="android.intent.action.MAIN"/>
                <category android:name="android.intent.category.LAUNCHER"/>
            </intent-filter>
        </activity>
    </application>
</manifest>
"""

private const val MAIN_ACTIVITY = """package {{packageName}}

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import {{packageName}}.screens.MainScreen
import {{packageName}}.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                MainScreen()
            }
        }
    }
}
"""

private const val THEME_KT = """package {{packageName}}.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(primary = Primary, secondary = Secondary),
        content = content
    )
}
"""

private const val COLOR_KT = """package {{packageName}}.ui.theme

import androidx.compose.ui.graphics.Color

val Primary = Color(0xFF6750A4)
val Secondary = Color(0xFF958DA5)
"""

private const val MAIN_SCREEN = """package {{packageName}}.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MainScreen() {
    Scaffold(
        topBar = { TopAppBar(title = { Text("{{projectName}}") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = "Hello!",
                style = MaterialTheme.typography.headlineMedium
            )
        }
    }
}
"""

private const val STRINGS_XML = """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">{{projectName}}</string>
</resources>
"""

private const val THEMES_XML = """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.App" parent="android:Theme.Material.Light.NoActionBar"/>
</resources>
"""

private const val README = """# {{projectName}}

Jetpack Compose проект, сгенерированный в Ai IDE.

## Сборка

Откройте в Android Studio или выполните `./gradlew :app:assembleDebug`.
"""
