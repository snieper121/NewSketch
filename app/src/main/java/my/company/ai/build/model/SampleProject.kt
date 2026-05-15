package my.company.ai.build.model

import java.io.File

/**
 * Hardcoded Hello World проект для M0 PoC.
 * Без JSON, без editor — чистый Kotlin-код для проверки pipeline.
 */
object SampleProject {

    const val PACKAGE = "com.example.helloworld"
    const val MIN_SDK = 29
    const val TARGET_SDK = 36

    fun writeTo(projectDir: File) {
        val srcDir = File(projectDir, "src/main/java/com/example/helloworld").apply { mkdirs() }
        val resDir = File(projectDir, "src/main/res/values").apply { mkdirs() }
        val manifestFile = File(projectDir, "src/main/AndroidManifest.xml")

        File(srcDir, "MainActivity.kt").writeText(MAIN_ACTIVITY.trimIndent())
        File(srcDir, "ui/theme/Theme.kt").apply { parentFile?.mkdirs() }.writeText(THEME.trimIndent())
        File(srcDir, "ui/theme/Color.kt").apply { parentFile?.mkdirs() }.writeText(COLOR.trimIndent())
        File(srcDir, "screens/MainScreen.kt").apply { parentFile?.mkdirs() }.writeText(MAIN_SCREEN.trimIndent())
        File(resDir, "strings.xml").writeText(STRINGS_XML.trimIndent())
        File(resDir, "themes.xml").writeText(THEMES_XML.trimIndent())
        manifestFile.writeText(MANIFEST.trimIndent())
    }

    private val MAIN_ACTIVITY = """
        package com.example.helloworld

        import android.os.Bundle
        import androidx.activity.ComponentActivity
        import androidx.activity.compose.setContent
        import androidx.activity.enableEdgeToEdge
        import com.example.helloworld.screens.MainScreen
        import com.example.helloworld.ui.theme.AppTheme

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

    private val THEME = """
        package com.example.helloworld.ui.theme

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

    private val COLOR = """
        package com.example.helloworld.ui.theme

        import androidx.compose.ui.graphics.Color

        val Primary = Color(0xFF6750A4)
        val Secondary = Color(0xFF958DA5)
    """

    private val MAIN_SCREEN = """
        package com.example.helloworld.screens

        import androidx.compose.foundation.layout.*
        import androidx.compose.material3.*
        import androidx.compose.runtime.*
        import androidx.compose.ui.Alignment
        import androidx.compose.ui.Modifier
        import androidx.compose.ui.unit.dp

        @Composable
        fun MainScreen() {
            Scaffold(
                topBar = { TopAppBar(title = { Text("Hello World") }) }
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Hello from Ai IDE!",
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
            }
        }
    """

    private val MANIFEST = """<?xml version="1.0" encoding="utf-8"?>
        <manifest xmlns:android="http://schemas.android.com/apk/res/android"
            package="com.example.helloworld">
            <application
                android:allowBackup="true"
                android:label="Hello World"
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
        </manifest>"""

    private val STRINGS_XML = """<?xml version="1.0" encoding="utf-8"?>
        <resources>
            <string name="app_name">Hello World</string>
        </resources>"""

    private val THEMES_XML = """<?xml version="1.0" encoding="utf-8"?>
        <resources>
            <style name="Theme.App" parent="android:Theme.Material.Light.NoActionBar"/>
        </resources>"""
}
