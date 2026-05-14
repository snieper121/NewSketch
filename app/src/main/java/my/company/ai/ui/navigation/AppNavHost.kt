package my.company.ai.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import my.company.ai.ui.screens.build.BuildScreen
import my.company.ai.ui.screens.chat.ChatScreen
import my.company.ai.ui.screens.editor.EditorScreen
import my.company.ai.ui.screens.projects.ProjectsScreen
import my.company.ai.ui.screens.projectsettings.ProjectSettingsScreen
import my.company.ai.ui.screens.resources.ResourceManagerScreen
import my.company.ai.ui.screens.screens.ScreenManagerScreen
import my.company.ai.ui.screens.settings.SettingsScreen
import my.company.ai.ui.screens.statelogic.StateLogicScreen

object Routes {
    const val PROJECTS = "projects"
    const val EDITOR = "editor/{projectId}"
    const val BUILD = "build/{projectId}"
    const val CHAT = "chat/{projectId}"
    const val SETTINGS = "settings"
    const val PROJECT_SETTINGS = "project_settings/{projectId}"
    const val SCREEN_MANAGER = "screen_manager/{projectId}"
    const val RESOURCES = "resources/{projectId}"
    const val STATE_LOGIC = "state_logic/{projectId}"

    fun editor(projectId: String) = "editor/$projectId"
    fun build(projectId: String) = "build/$projectId"
    fun chat(projectId: String) = "chat/$projectId"
    fun projectSettings(projectId: String) = "project_settings/$projectId"
    fun screenManager(projectId: String) = "screen_manager/$projectId"
    fun resources(projectId: String) = "resources/$projectId"
    fun stateLogic(projectId: String) = "state_logic/$projectId"
}

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(navController = navController, startDestination = Routes.PROJECTS, modifier = modifier) {

        composable(Routes.PROJECTS) {
            ProjectsScreen(
                onOpenProject = { navController.navigate(Routes.editor(it)) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }

        composable(Routes.EDITOR, arguments = listOf(navArgument("projectId") { type = NavType.StringType })) { entry ->
            val id = entry.arguments!!.getString("projectId")!!
            EditorScreen(
                projectId = id,
                onBack = { navController.popBackStack() },
                onOpenChat = { navController.navigate(Routes.chat(id)) },
            )
        }

        composable(Routes.BUILD, arguments = listOf(navArgument("projectId") { type = NavType.StringType })) { entry ->
            val id = entry.arguments!!.getString("projectId")!!
            BuildScreen(projectId = id, onBack = { navController.popBackStack() })
        }

        composable(Routes.CHAT, arguments = listOf(navArgument("projectId") { type = NavType.StringType })) { entry ->
            val id = entry.arguments!!.getString("projectId")!!
            ChatScreen(projectId = id, onBack = { navController.popBackStack() })
        }

        composable(Routes.PROJECT_SETTINGS, arguments = listOf(navArgument("projectId") { type = NavType.StringType })) { entry ->
            val id = entry.arguments!!.getString("projectId")!!
            ProjectSettingsScreen(projectId = id, onBack = { navController.popBackStack() })
        }

        composable(Routes.SCREEN_MANAGER, arguments = listOf(navArgument("projectId") { type = NavType.StringType })) { entry ->
            val id = entry.arguments!!.getString("projectId")!!
            ScreenManagerScreen(projectId = id, onBack = { navController.popBackStack() }, onEditScreen = { /* TODO */ })
        }

        composable(Routes.RESOURCES, arguments = listOf(navArgument("projectId") { type = NavType.StringType })) { entry ->
            val id = entry.arguments!!.getString("projectId")!!
            ResourceManagerScreen(projectId = id, onBack = { navController.popBackStack() })
        }

        composable(Routes.STATE_LOGIC, arguments = listOf(navArgument("projectId") { type = NavType.StringType })) { entry ->
            val id = entry.arguments!!.getString("projectId")!!
            StateLogicScreen(projectId = id, onBack = { navController.popBackStack() })
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
