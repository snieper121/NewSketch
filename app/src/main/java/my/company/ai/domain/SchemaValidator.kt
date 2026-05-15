package my.company.ai.domain

import my.company.ai.domain.model.ProjectJson
import my.company.ai.domain.model.ScreenJson
import my.company.ai.domain.model.StateJson
import my.company.ai.domain.model.WidgetJson

/**
 * Валидатор JSON-модели проекта.
 *
 * Проверяет:
 * - Уникальность ID экранов, виджетов, состояния
 * - Наличие хотя бы одного стартового экрана
 * - Валидность package name
 * - Корректность типов состояния
 */
object SchemaValidator {

    sealed class ValidationError {
        abstract val message: String

        data class DuplicateId(override val message: String) : ValidationError()
        data class MissingStartScreen(override val message: String) : ValidationError()
        data class InvalidPackageName(override val message: String) : ValidationError()
        data class InvalidStateType(override val message: String) : ValidationError()
        data class EmptyProjectName(override val message: String) : ValidationError()
    }

    fun validate(project: ProjectJson): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        // Package name
        if (!project.packageName.matches(Regex("""^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$"""))) {
            errors += ValidationError.InvalidPackageName(
                "packageName '${project.packageName}' не соответствует формату Java package"
            )
        }

        // Screens
        val screenIds = mutableSetOf<String>()
        var hasStartScreen = false
        project.screens.forEach { screen ->
            if (!screenIds.add(screen.id)) {
                errors += ValidationError.DuplicateId("Дубликат id экрана: ${screen.id}")
            }
            if (screen.isStart) hasStartScreen = true
            errors += validateScreen(screen)
        }

        if (!hasStartScreen && project.screens.isNotEmpty()) {
            errors += ValidationError.MissingStartScreen("Нет стартового экрана (isStart = true)")
        }

        // State
        val stateIds = mutableSetOf<String>()
        val validStateTypes = setOf("String", "Int", "Boolean", "Float", "List", "Map")
        project.state.forEach { state ->
            if (!stateIds.add(state.id)) {
                errors += ValidationError.DuplicateId("Дубликат id состояния: ${state.id}")
            }
            if (state.type !in validStateTypes) {
                errors += ValidationError.InvalidStateType(
                    "Недопустимый тип состояния '${state.type}'. Допустимые: $validStateTypes"
                )
            }
        }

        return errors
    }

    private fun validateScreen(screen: ScreenJson): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        val widgetIds = mutableSetOf<String>()
        screen.widgets.forEach { widget ->
            errors += validateWidget(widget, widgetIds)
        }
        return errors
    }

    private fun validateWidget(widget: WidgetJson, ids: MutableSet<String>): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        if (!ids.add(widget.id)) {
            errors += ValidationError.DuplicateId("Дубликат id виджета: ${widget.id}")
        }
        widget.children.forEach { child ->
            errors += validateWidget(child, ids)
        }
        return errors
    }
}
