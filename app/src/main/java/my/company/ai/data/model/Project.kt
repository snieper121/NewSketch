package my.company.ai.data.model

/**
 * Метаданные пользовательского Kotlin-проекта, создаваемого в редакторе.
 *
 * Исходники хранятся на файловой системе под [rootPath], а запись в БД (Room)
 * нужна для быстрого листинга и сортировки (ADR-005).
 */
data class Project(
    val id: String,
    val name: String,
    val packageName: String,
    val rootPath: String,
    val createdAt: Long,
    val updatedAt: Long,
) {
    companion object {
        /** Минимальный валидный Android package name: сегменты, разделённые точкой. */
        private val PACKAGE_REGEX = Regex("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+$")

        fun isValidPackageName(value: String): Boolean = PACKAGE_REGEX.matches(value)
    }
}
