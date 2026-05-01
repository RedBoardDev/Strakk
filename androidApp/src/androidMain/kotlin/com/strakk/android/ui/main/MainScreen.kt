package com.strakk.android.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import com.strakk.android.ui.calendar.CalendarRoute
import com.strakk.android.ui.home.draft.MealDraftRoute
import com.strakk.android.ui.home.manual.ManualEntryRoute
import com.strakk.android.ui.home.photo.PhotoHintScreen
import com.strakk.android.ui.home.review.MealReviewRoute
import com.strakk.android.ui.home.search.SearchFoodRoute
import com.strakk.android.ui.home.text.TextEntryScreen
import com.strakk.android.ui.settings.SettingsRoute
import com.strakk.android.ui.theme.LocalStrakkColors
import com.strakk.android.ui.theme.StrakkTheme
import com.strakk.android.ui.today.TodayRoute
import com.strakk.shared.domain.model.DraftItem
import com.strakk.shared.domain.model.MealEntry
import com.strakk.shared.presentation.meal.MealDraftEvent
import com.strakk.shared.presentation.meal.MealDraftViewModel
import com.strakk.shared.presentation.meal.QuickAddEvent
import com.strakk.shared.presentation.meal.QuickAddViewModel
import org.koin.compose.viewmodel.koinViewModel

// =============================================================================
// In-app route definitions (no external nav library)
// =============================================================================

sealed interface HomeRoute {
    data object Today : HomeRoute
    data object Draft : HomeRoute
    data object Review : HomeRoute
    /** @param inDraft true → result goes to draft; false → quick-add */
    data class Search(val inDraft: Boolean) : HomeRoute
    data class Manual(val inDraft: Boolean) : HomeRoute
    data class Photo(val inDraft: Boolean) : HomeRoute
    data class TextEntry(val inDraft: Boolean) : HomeRoute
}

private data class TabItem(
    val label: String,
    val icon: ImageVector,
)

private val tabs = listOf(
    TabItem(label = "Today", icon = Icons.Outlined.Home),
    TabItem(label = "Calendar", icon = Icons.Outlined.CalendarMonth),
    TabItem(label = "Settings", icon = Icons.Outlined.Settings),
)

// =============================================================================
// MainScreen
// =============================================================================

@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    var selectedTab by remember { mutableIntStateOf(0) }

    // Simple stack for home sub-navigation (Today tab only)
    val homeBackStack = remember { androidx.compose.runtime.mutableStateListOf<HomeRoute>(HomeRoute.Today) }
    val currentHomeRoute = homeBackStack.lastOrNull() ?: HomeRoute.Today

    // Hide the bottom bar when navigating away from Today root
    val showBottomBar = currentHomeRoute == HomeRoute.Today

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = LocalStrakkColors.current.surface2) {
                    tabs.forEachIndexed { index, tab ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = {
                                selectedTab = index
                                // Reset home back stack when switching tabs
                                if (index == 0) {
                                    homeBackStack.clear()
                                    homeBackStack.add(HomeRoute.Today)
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = LocalStrakkColors.current.textSecondary,
                                unselectedTextColor = LocalStrakkColors.current.textSecondary,
                                indicatorColor = LocalStrakkColors.current.surface2,
                            ),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        val contentModifier = Modifier.padding(innerPadding)
        when (selectedTab) {
            0 -> HomeTabContent(
                backStack = homeBackStack,
                currentRoute = currentHomeRoute,
                modifier = contentModifier,
            )
            1 -> CalendarRoute(modifier = contentModifier)
            2 -> SettingsRoute(modifier = contentModifier)
        }
    }
}

// =============================================================================
// Home tab — routing based on back stack
// =============================================================================

@Composable
private fun HomeTabContent(
    backStack: SnapshotStateList<HomeRoute>,
    currentRoute: HomeRoute,
    modifier: Modifier = Modifier,
) {
    // Shared Koin-scoped ViewModels used to dispatch results from
    // the Add flow back into either the active Draft or an orphan quick-add.
    val draftViewModel: MealDraftViewModel = koinViewModel()
    val quickAddViewModel: QuickAddViewModel = koinViewModel()

    fun push(route: HomeRoute) { backStack.add(route) }
    fun pop() { if (backStack.size > 1) backStack.removeLastOrNull() }
    fun popToToday() { backStack.clear(); backStack.add(HomeRoute.Today) }

    fun dispatchEntry(inDraft: Boolean, entry: MealEntry) {
        if (inDraft) {
            draftViewModel.onEvent(
                MealDraftEvent.AddResolvedItem(
                    item = DraftItem.Resolved(
                        id = generateDraftItemId(),
                        entry = entry,
                    ),
                ),
            )
        } else {
            quickAddViewModel.onEvent(
                QuickAddEvent.AddKnown(
                    name = entry.name ?: "",
                    protein = entry.protein,
                    calories = entry.calories,
                    fat = entry.fat,
                    carbs = entry.carbs,
                    quantity = entry.quantity,
                    source = entry.source,
                ),
            )
        }
    }

    when (currentRoute) {
        is HomeRoute.Today -> TodayRoute(
            onNavigateToDraft = { push(HomeRoute.Draft) },
            onNavigateToQuickAdd = { push(HomeRoute.Search(inDraft = false)) },
            onDiscardDraft = { draftViewModel.onEvent(MealDraftEvent.Discard) },
            modifier = modifier,
        )

        is HomeRoute.Draft -> MealDraftRoute(
            onNavigateBack = { pop() },
            onNavigateToReview = { push(HomeRoute.Review) },
            onNavigateToSearch = { inDraft -> push(HomeRoute.Search(inDraft)) },
            onNavigateToManual = { inDraft -> push(HomeRoute.Manual(inDraft)) },
            onNavigateToPhoto = { inDraft -> push(HomeRoute.Photo(inDraft)) },
            onNavigateToText = { inDraft -> push(HomeRoute.TextEntry(inDraft)) },
            viewModel = draftViewModel,
            modifier = modifier,
        )

        is HomeRoute.Review -> MealReviewRoute(
            onNavigateBack = { pop() },
            onCommitted = { popToToday() },
            viewModel = draftViewModel,
            modifier = modifier,
        )

        is HomeRoute.Search -> SearchFoodRoute(
            onNavigateBack = { pop() },
            onConfirm = { entry ->
                dispatchEntry(currentRoute.inDraft, entry)
                pop()
            },
            modifier = modifier,
        )

        is HomeRoute.Manual -> ManualEntryRoute(
            onDismiss = { pop() },
            onAdded = { entry ->
                dispatchEntry(currentRoute.inDraft, entry)
                pop()
            },
            modifier = modifier,
        )

        is HomeRoute.Photo -> PhotoHintScreen(
            onNavigateBack = { pop() },
            onSubmit = { base64, hint ->
                if (currentRoute.inDraft) {
                    draftViewModel.onEvent(
                        MealDraftEvent.AddPendingPhoto(imageBase64 = base64, hint = hint),
                    )
                } else {
                    quickAddViewModel.onEvent(
                        QuickAddEvent.AddFromPhoto(imageBase64 = base64, hint = hint),
                    )
                }
                pop()
            },
            modifier = modifier,
        )

        is HomeRoute.TextEntry -> TextEntryScreen(
            onNavigateBack = { pop() },
            onSubmit = { description ->
                if (currentRoute.inDraft) {
                    draftViewModel.onEvent(
                        MealDraftEvent.AddPendingText(description = description),
                    )
                } else {
                    quickAddViewModel.onEvent(
                        QuickAddEvent.AddFromText(description = description),
                    )
                }
                pop()
            },
            modifier = modifier,
        )
    }
}

private fun generateDraftItemId(): String =
    "item-${System.currentTimeMillis()}-${(0..0xFFFF).random().toString(16)}"

// =============================================================================
// Preview
// =============================================================================

@Preview(showBackground = true, backgroundColor = 0xFF151720)
@Composable
private fun MainScreenPreview() {
    StrakkTheme {
        Scaffold(
            bottomBar = {
                NavigationBar(containerColor = LocalStrakkColors.current.surface2) {
                    tabs.forEachIndexed { index, tab ->
                        NavigationBarItem(
                            selected = index == 0,
                            onClick = {},
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            },
        ) { innerPadding ->
            TodayRoute(
                onNavigateToDraft = {},
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}
