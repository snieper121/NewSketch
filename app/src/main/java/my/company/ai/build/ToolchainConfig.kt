package my.company.ai.build

/**
 * Конфигурация toolchain для on-device сборки.
 *
 * Все URL указывают на GitHub Releases репозитория ai-ide-toolchain.
 * SHA-256 будут вычислены при первой загрузке и зафиксированы здесь.
 */
object ToolchainConfig {
    const val KOTLIN_VERSION = "2.2.0"
    const val COMPOSE_COMPILER_VERSION = "2.2.0"
    const val ECJ_VERSION = "3.40.0"
    const val BUILD_TOOLS_VERSION = "35.0.0"
    const val PLATFORM_API = 35
    const val R8_VERSION = "8.10.22"

    const val CDN_BASE = "https://github.com/snieper121/ai-ide-toolchain/releases/download/v1/"

    data class Tool(
        val filename: String,
        val url: String,
        val sha256: String,
        val expectedSize: Long,
        val isExecutable: Boolean = false,
    )

    val TOOLS = listOf(
        Tool("kotlinc.dex", "${CDN_BASE}kotlinc-${KOTLIN_VERSION}.dex", "TODO", 62_000_000L),
        Tool("compose-plugin.dex", "${CDN_BASE}compose-plugin-${COMPOSE_COMPILER_VERSION}.dex", "TODO", 5_000_000L),
        Tool("ecj.dex", "${CDN_BASE}ecj-${ECJ_VERSION}.dex", "TODO", 2_100_000L),
        Tool("r8.dex", "${CDN_BASE}r8-${R8_VERSION}.dex", "TODO", 5_000_000L),
        Tool("apksigner.dex", "${CDN_BASE}apksigner-${BUILD_TOOLS_VERSION}.dex", "TODO", 2_000_000L),
        Tool("aapt2", "${CDN_BASE}aapt2-${BUILD_TOOLS_VERSION}-arm64", "TODO", 5_000_000L, isExecutable = true),
        Tool("zipalign", "${CDN_BASE}zipalign-${BUILD_TOOLS_VERSION}-arm64", "TODO", 1_000_000L, isExecutable = true),
        Tool("android.jar", "${CDN_BASE}android-${PLATFORM_API}.jar", "TODO", 30_000_000L),
        Tool("kotlin-stdlib.jar", "${CDN_BASE}kotlin-stdlib-${KOTLIN_VERSION}.jar", "TODO", 2_000_000L),
    )

    val CLASSPATH_JARS = listOf(
        "android.jar",
        "kotlin-stdlib.jar",
    )
}
