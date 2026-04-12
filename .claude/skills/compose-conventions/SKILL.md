---
description: "Jetpack Compose/Android conventions for androidApp"
paths:
  - "androidApp/**/*.kt"
---

# Jetpack Compose / Android Conventions for Strakk

## Route / Screen / Content Pattern

```
Route (stateful)         — koinViewModel(), collectAsStateWithLifecycle(), effects
  Screen (stateless)     — receives state + callbacks, Modifier last param
    Content (leaf)       — renders sub-state, emits callbacks
```

### Route
```kotlin
@Composable
fun FeatureRoute(
    onNavigateTo: (destination) -> Unit,
    viewModel: FeatureViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    FeatureScreen(uiState = uiState, onEvent = viewModel::onEvent, modifier = Modifier)
}
```

### Screen
```kotlin
@Composable
fun FeatureScreen(
    uiState: FeatureUiState,
    onEvent: (FeatureEvent) -> Unit,
    modifier: Modifier = Modifier, // ALWAYS LAST
) { ... }
```

## State Collection

```kotlin
// CORRECT — lifecycle-aware
val uiState by viewModel.uiState.collectAsStateWithLifecycle()

// WRONG — not lifecycle-safe, wastes resources
val uiState by viewModel.uiState.collectAsState()
```

## Composable Parameter Order

1. Required data parameters
2. Required callback parameters
3. Optional data parameters with defaults
4. `modifier: Modifier = Modifier` (ALWAYS LAST)

## Navigation

### Current: Navigation 2 (Type-Safe)

The project currently uses Navigation 2 with type-safe routes. This is stable and works.

```kotlin
@Serializable data object HomeRoute
@Serializable data class DetailRoute(val id: Long)

// NEVER use string routes: composable("detail/{id}")
```

### Migration path: Navigation 3 (stable since Nov 2025)

Navigation 3 is stable (androidx.navigation3:navigation3:1.0.1+) and supported in
Compose Multiplatform 1.10+. It uses a composable back stack instead of NavHost/NavController.
Migrate when we upgrade to Compose Multiplatform 1.10+.

```kotlin
// Navigation 3 keys implement NavKey + @Serializable
@Serializable data object Home : NavKey
@Serializable data class SessionDetail(val id: String) : NavKey

@Composable
fun StrakkNavHost() {
    val backStack = rememberNavBackStack<NavKey>(Home)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<Home> {
                HomeRoute(onNavigateToSession = { id -> backStack.add(SessionDetail(id)) })
            }
            entry<SessionDetail> { key ->
                SessionDetailRoute(sessionId = key.id)
            }
        },
    )
}
```

Key differences from Nav 2:
- Back stack is a `SnapshotStateList` you own — `add()` to push, `removeLastOrNull()` to pop
- No NavController — direct list manipulation
- `rememberNavBackStack` survives config change + process death
- `rememberViewModelStoreNavEntryDecorator()` scopes ViewModels to entries

## Edge-to-Edge (Mandatory on Android 16 / API 36)

Android 16 enforces edge-to-edge — the `windowOptOutEdgeToEdgeEnforcement` escape hatch is removed.
When we target API 36, the app MUST handle insets correctly.

```kotlin
// In Activity.onCreate — already in place
enableEdgeToEdge()

// In Compose — ALWAYS use Scaffold's innerPadding, never hardcode padding
Scaffold { innerPadding ->
    Content(modifier = Modifier.padding(innerPadding))
}

// For non-Scaffold screens, consume insets explicitly
Modifier
    .fillMaxSize()
    .windowInsetsPadding(WindowInsets.safeDrawing)
```

**Rules:**
- NEVER hardcode status/navigation bar padding (24.dp, 48.dp, etc.)
- NEVER use deprecated Accompanist SystemUiController — use `enableEdgeToEdge()`
- Scaffold handles innerPadding — pass it to content, do not ignore it

## Material 3 Theming

- Use `MaterialTheme.colorScheme.primary` etc. — NEVER hardcode hex
- `MaterialTheme.typography.headlineMedium` etc.
- Dynamic color on API 31+
- Custom tokens via `staticCompositionLocalOf` if needed

## Material 3 Expressive Components (M3 1.4+)

These components require `@OptIn(ExperimentalMaterial3ExpressiveApi::class)` and are
available in compose-bom 2026.03.00+ / material3 1.4+. Use for fitness-relevant UI:

### LoadingIndicator — replaces CircularProgressIndicator for short waits
```kotlin
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RestTimerLoading() {
    // Fluid morphing shapes — better for rest timers, sync indicators
    LoadingIndicator()

    // Contained variant for cards/buttons
    ContainedLoadingIndicator()
}
```
Use `LoadingIndicator` for rest timers, sync status. Keep `CircularProgressIndicator` for
determinate progress (e.g., workout completion percentage).

### FloatingToolbar — workout session controls
```kotlin
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WorkoutToolbar(
    onTimerClick: () -> Unit,
    onAddExercise: () -> Unit,
    onFinish: () -> Unit,
) {
    HorizontalFloatingToolbar(
        expanded = true,
        floatingActionButton = {
            FloatingActionButton(onClick = onFinish) {
                Icon(Icons.Default.Check, contentDescription = "Finish workout")
            }
        },
        content = {
            IconButton(onClick = onTimerClick) {
                Icon(Icons.Default.Timer, contentDescription = "Timer")
            }
            IconButton(onClick = onAddExercise) {
                Icon(Icons.Default.Add, contentDescription = "Add exercise")
            }
        },
    )
}
```
Ideal for active workout screens — collapses on scroll, integrates with FAB.

### ButtonGroup — useful for rep/set selectors
```kotlin
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SetTypeSelector(
    selected: SetType,
    onSelect: (SetType) -> Unit,
) {
    ButtonGroup {
        SetType.entries.forEach { type ->
            ToggleButton(
                checked = type == selected,
                onCheckedChange = { onSelect(type) },
            ) {
                Text(type.label)
            }
        }
    }
}
```

## Lists

```kotlin
LazyColumn {
    items(
        items = list,
        key = { it.id }, // ALWAYS provide stable key
    ) { item ->
        ItemRow(item = item)
    }
}
```

## DI

```kotlin
val viewModel: FeatureViewModel = koinViewModel()

// With parameters
val viewModel: DetailViewModel = koinViewModel { parametersOf(itemId) }
```

Import from `org.koin.compose.viewmodel.koinViewModel` (koin-compose-viewmodel artifact).

## Performance

### Strong Skipping (enabled by default since Compose Compiler 1.5.4+)

Strong skipping lets composables with unstable parameters generate skipping code.
Our Compose Compiler (via Kotlin 2.1.20 plugin) has this on by default — no config needed.

**What this means in practice:**
- You no longer need `@Stable`/`@Immutable` on every data class just for skipping
- Lambdas inside composables are automatically remembered
- Still prefer `List<T>` over `MutableList<T>` in state — strong skipping helps, but stable types are still faster

### Pausable Composition (enabled by default in Compose 1.10+)

Lazy prefetch now pauses composition when a frame deadline approaches, reducing jank
during heavy list scrolling. No code changes needed — just keep `key` on lazy items.

### Performance Rules
- Pass **lambdas** instead of raw state values to children when the value changes frequently
- Use `Modifier.animateItem()` on lazy list items (replaces deprecated `animateItemPlacement`)
- Prefer `derivedStateOf` for computed values that shouldn't trigger recomposition on every change
- NEVER write to state during Composition — always in callbacks or LaunchedEffect

## Anti-Patterns

| Bad | Good |
|-----|------|
| `collectAsState()` | `collectAsStateWithLifecycle()` |
| `NavController` in Screen | Hoist `onNavigate` callbacks |
| `MutableList` in state | `List<T>` |
| `material-icons-extended` | Individual icon imports |
| Hardcoded colors | `MaterialTheme.colorScheme.*` |
| String navigation routes | `@Serializable` data object/class routes |
| Business logic in Composable | Delegate to ViewModel |
| `CircularProgressIndicator` for short waits | `LoadingIndicator` (M3 Expressive) |
| Hardcoded inset padding (24.dp) | `Scaffold` innerPadding or `WindowInsets.safeDrawing` |
| `animateItemPlacement` (deprecated) | `Modifier.animateItem()` |
| Accompanist SystemUiController | `enableEdgeToEdge()` |

## Style

- Trailing commas on all multiline params
- One Composable per file for screens
- Previews with `@Preview` for Screen-level composables
- Compose Compiler is a Gradle plugin — do NOT add compiler dependency manually

## Version Reference (April 2026)

| Dependency | Current | Latest Stable |
|---|---|---|
| compose-bom | 2024.12.01 | **2026.03.00** |
| compose-multiplatform | 1.7.3 | **1.10.3** (1.11.0 beta) |
| kotlin | 2.1.20 | 2.1.20 (current) |
| koin | 4.0.0 | 4.0.x (current) |
| lifecycle | 2.8.4 | 2.9.x |
| navigation3 | — | 1.0.1 |

Upgrade path: bump `compose-bom` to 2026.03.00 and `compose-multiplatform` to 1.10.3
to unlock M3 Expressive components, Navigation 3, and pausable composition.

## References

- For complete Route/Screen/Content pattern, see [references/route-screen-content-pattern.kt](references/route-screen-content-pattern.kt)
- For Material 3 theming setup, see [references/material3-theming.kt](references/material3-theming.kt)
- For Navigation 3 setup with bottom tabs, see [references/navigation3-setup.kt](references/navigation3-setup.kt)
