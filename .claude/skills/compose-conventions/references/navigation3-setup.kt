package com.strakk.androidApp.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.NavDisplay
import androidx.navigation3.NavKey
import androidx.navigation3.rememberNavBackStack
import androidx.navigation3.entryProvider
import androidx.navigation3.entry
import androidx.navigation3.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.rememberViewModelStoreNavEntryDecorator
import kotlinx.serialization.Serializable

// =============================================================================
// Step 1: Define NavKey routes (replaces @Serializable data objects from Nav 2)
// =============================================================================

@Serializable
data object Home : NavKey

@Serializable
data object Sessions : NavKey

@Serializable
data object Progress : NavKey

@Serializable
data object Profile : NavKey

@Serializable
data class SessionDetail(val id: String) : NavKey

@Serializable
data class ExerciseDetail(val sessionId: String, val exerciseId: String) : NavKey

// =============================================================================
// Step 2: Bottom navigation tabs
// =============================================================================

enum class BottomTab(
    val label: String,
    val icon: ImageVector,
    val route: NavKey,
) {
    HOME("Home", Icons.Default.Home, Home),
    SESSIONS("Sessions", Icons.Default.FitnessCenter, Sessions),
    PROGRESS("Progress", Icons.Default.ShowChart, Progress),
    PROFILE("Profile", Icons.Default.Person, Profile),
}

// =============================================================================
// Step 3: NavDisplay setup with bottom tabs
// =============================================================================

/// Navigation 3 uses a composable back stack — no NavController needed.
/// The back stack is a SnapshotStateList you own and manipulate directly.
@Composable
fun StrakkNavHost() {
    val backStack = rememberNavBackStack<NavKey>(Home)

    Scaffold(
        bottomBar = {
            StrakkBottomBar(
                currentRoute = backStack.lastOrNull() ?: Home,
                onTabSelected = { tab ->
                    // Clear to root and navigate to tab
                    while (backStack.size > 1) {
                        backStack.removeLastOrNull()
                    }
                    if (backStack.lastOrNull() != tab.route) {
                        backStack.removeLastOrNull()
                        backStack.add(tab.route)
                    }
                },
            )
        },
    ) { innerPadding ->
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            entryDecorators = listOf(
                // Preserves state across configuration changes
                rememberSaveableStateHolderNavEntryDecorator(),
                // Scopes ViewModels to navigation entries (auto-cleared on pop)
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = entryProvider {
                entry<Home> {
                    HomeRoute(
                        onNavigateToSession = { id ->
                            backStack.add(SessionDetail(id))
                        },
                    )
                }
                entry<Sessions> {
                    SessionListRoute(
                        onNavigateToSession = { id ->
                            backStack.add(SessionDetail(id))
                        },
                    )
                }
                entry<Progress> {
                    ProgressRoute()
                }
                entry<Profile> {
                    ProfileRoute()
                }
                entry<SessionDetail> { key ->
                    SessionDetailRoute(
                        sessionId = key.id,
                        onNavigateToExercise = { exerciseId ->
                            backStack.add(ExerciseDetail(key.id, exerciseId))
                        },
                        onBack = { backStack.removeLastOrNull() },
                    )
                }
                entry<ExerciseDetail> { key ->
                    ExerciseDetailRoute(
                        sessionId = key.sessionId,
                        exerciseId = key.exerciseId,
                        onBack = { backStack.removeLastOrNull() },
                    )
                }
            },
        )
    }
}

// =============================================================================
// Step 4: Bottom bar composable
// =============================================================================

@Composable
private fun StrakkBottomBar(
    currentRoute: NavKey,
    onTabSelected: (BottomTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(modifier = modifier) {
        BottomTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = currentRoute == tab.route,
                onClick = { onTabSelected(tab) },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label) },
            )
        }
    }
}

// =============================================================================
// Key differences from Navigation 2:
// =============================================================================
//
// | Navigation 2                          | Navigation 3                                |
// |---------------------------------------|---------------------------------------------|
// | NavHost + NavController               | NavDisplay + SnapshotStateList backStack     |
// | navController.navigate(Route)         | backStack.add(Route)                        |
// | navController.popBackStack()          | backStack.removeLastOrNull()                |
// | composable<Route> { ... }             | entry<Route> { key -> ... }                 |
// | NavController survives config change  | rememberNavBackStack survives config+death   |
// | ViewModel scoped via hiltViewModel()  | rememberViewModelStoreNavEntryDecorator()   |
// | NavBackStackEntry.arguments           | Entry key is the @Serializable data class   |
//
// Migration steps:
// 1. Replace NavHost -> NavDisplay
// 2. Replace NavController -> rememberNavBackStack<NavKey>(startKey)
// 3. Replace composable<Route> { } -> entry<Route> { key -> }
// 4. Replace navigate() -> backStack.add()
// 5. Replace popBackStack() -> backStack.removeLastOrNull()
// 6. Add entryDecorators for state and ViewModel scoping
