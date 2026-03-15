package com.cuchieman.tamatask.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Main : Screen("main")
    data object TaskCreate : Screen("task_create")
    data object TaskDetail : Screen("task_detail/{taskId}") {
        fun createRoute(taskId: Long) = "task_detail/$taskId"
    }
}

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Pet : BottomNavItem("pet", "Mascota", Icons.Filled.Pets)
    data object Tasks : BottomNavItem("tasks", "Tareas", Icons.Filled.CheckCircle)
    data object Collection : BottomNavItem("collection", "Coleccion", Icons.Filled.CollectionsBookmark)
    data object Profile : BottomNavItem("profile", "Perfil", Icons.Filled.Person)
}
