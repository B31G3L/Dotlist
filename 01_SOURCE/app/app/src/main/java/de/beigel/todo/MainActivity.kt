package de.beigel.todo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import de.beigel.todo.data.DeviceIdManager
import de.beigel.todo.repository.TodoRepository
import de.beigel.todo.ui.screens.ListsScreen
import de.beigel.todo.ui.screens.TodosScreen
import de.beigel.todo.ui.theme.TodoSharedTheme
import de.beigel.todo.viewmodel.ListsViewModel
import de.beigel.todo.viewmodel.TodosViewModel
import java.net.URLDecoder
import java.net.URLEncoder

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val deviceId = DeviceIdManager.getDeviceId(this)
        val repository = TodoRepository(deviceId)

        setContent {
            TodoSharedTheme {
                TodoApp(repository = repository, deviceId = deviceId)
            }
        }
    }
}

@Composable
fun TodoApp(repository: TodoRepository, deviceId: String) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "lists") {

        // Listenübersicht
        composable("lists") {
            val viewModel: ListsViewModel = viewModel(
                factory = ListsViewModel.Factory(repository)
            )
            ListsScreen(
                viewModel = viewModel,
                deviceId = deviceId,
                onOpenList = { list ->
                    val encodedName = URLEncoder.encode(list.name, "UTF-8")
                    val encodedColor = URLEncoder.encode(list.color, "UTF-8")
                    navController.navigate("todos/${list.id}/$encodedName/$encodedColor")
                }
            )
        }

        // Todo-Liste
        composable(
            route = "todos/{listId}/{listName}/{listColor}",
            arguments = listOf(
                navArgument("listId") { type = NavType.StringType },
                navArgument("listName") { type = NavType.StringType },
                navArgument("listColor") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val listId = backStackEntry.arguments?.getString("listId") ?: return@composable
            val listName = URLDecoder.decode(
                backStackEntry.arguments?.getString("listName") ?: "", "UTF-8"
            )
            val listColor = URLDecoder.decode(
                backStackEntry.arguments?.getString("listColor") ?: "#6750A4", "UTF-8"
            )

            val viewModel: TodosViewModel = viewModel(
                key = listId,
                factory = TodosViewModel.Factory(repository, listId)
            )

            TodosScreen(
                viewModel = viewModel,
                listName = listName,
                listColor = listColor,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
