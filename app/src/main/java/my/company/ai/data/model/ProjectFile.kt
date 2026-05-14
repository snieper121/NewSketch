package my.company.ai.data.model

/**
 * Узел дерева файлов проекта, отображаемый в редакторе.
 *
 * Путь [relativePath] — относительно корня проекта, чтобы было легко
 * сериализовать/сравнивать и не зависеть от конкретного filesDir устройства.
 */
data class ProjectFile(
    val relativePath: String,
    val isDirectory: Boolean,
    val children: List<ProjectFile> = emptyList(),
) {
    val name: String get() = relativePath.substringAfterLast('/')

    val extension: String get() = if (isDirectory) "" else name.substringAfterLast('.', "")

    val isKotlin: Boolean get() = extension.equals("kt", ignoreCase = true)

    val isEditable: Boolean
        get() = !isDirectory && extension.lowercase() in EDITABLE_EXTENSIONS

    companion object {
        private val EDITABLE_EXTENSIONS = setOf(
            "kt", "kts", "xml", "gradle", "properties", "toml",
            "md", "txt", "json", "yml", "yaml", "pro",
        )
    }
}
