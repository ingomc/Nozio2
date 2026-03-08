package de.ingomc.nozio

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import de.ingomc.nozio.data.local.MealType
import de.ingomc.nozio.data.repository.AppThemeMode
import de.ingomc.nozio.notifications.MealReminderScheduler
import de.ingomc.nozio.ui.dashboard.DashboardScreen
import de.ingomc.nozio.ui.dashboard.DashboardViewModel
import de.ingomc.nozio.ui.profile.LegalInfoScreen
import de.ingomc.nozio.ui.profile.ProfileEditGoalsScreen
import de.ingomc.nozio.ui.profile.ProfileScreen
import de.ingomc.nozio.ui.profile.ProfileViewModel
import de.ingomc.nozio.ui.search.SearchScreen
import de.ingomc.nozio.ui.search.SearchViewModel
import de.ingomc.nozio.ui.settings.SettingsScreen
import de.ingomc.nozio.ui.settings.SettingsSection
import de.ingomc.nozio.ui.settings.SettingsViewModel
import de.ingomc.nozio.ui.theme.NozioTheme
import de.ingomc.nozio.ui.theme.nozioColors
import de.ingomc.nozio.widget.CalorieWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val launchActionFlow = MutableStateFlow(WidgetLaunchAction.NONE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        launchActionFlow.value = extractLaunchAction(intent)
        setContent {
            val launchAction by launchActionFlow.collectAsState()
            NozioApp(
                launchAction = launchAction,
                onLaunchActionHandled = {
                    launchActionFlow.value = WidgetLaunchAction.NONE
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch(Dispatchers.IO) {
            CalorieWidgetProvider.updateAll(this@MainActivity)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        launchActionFlow.value = extractLaunchAction(intent)
    }

    companion object {
        fun createIntent(context: Context, launchAction: WidgetLaunchAction): Intent {
            return Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(WidgetLaunchAction.EXTRA_WIDGET_LAUNCH_ACTION, launchAction.extraValue)
        }
    }

    private fun extractLaunchAction(intent: Intent?): WidgetLaunchAction {
        return WidgetLaunchAction.fromExtraValue(
            intent?.getStringExtra(WidgetLaunchAction.EXTRA_WIDGET_LAUNCH_ACTION)
        )
    }
}

private object AppRoute {
    const val HOME = "home"
    const val SEARCH = "search"
    const val PROFILE = "profile"
    const val PROFILE_EDIT = "profile/edit"
    const val PROFILE_LEGAL = "profile/legal"
    const val SETTINGS_MAIN = "settings/main"
    const val SETTINGS_REMINDER = "settings/reminder"
    const val SETTINGS_BACKUP = "settings/backup"
}

@Composable
fun NozioApp(
    launchAction: WidgetLaunchAction = WidgetLaunchAction.NONE,
    onLaunchActionHandled: () -> Unit = {}
) {
    val app = LocalContext.current.applicationContext as NozioApplication
    val navController = rememberNavController()
    var preselectedMealType by rememberSaveable { mutableStateOf<MealType?>(null) }
    var openQuickAddOnStart by rememberSaveable { mutableStateOf(false) }
    var openBarcodeScannerOnStart by rememberSaveable { mutableStateOf(false) }
    var focusSearchOnStart by rememberSaveable { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }

    val userPreferences by app.userPreferencesRepository.userPreferences.collectAsState(initial = null)

    val dashboardViewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModel.Factory(
            app.applicationContext,
            app.diaryRepository,
            app.userPreferencesRepository,
            app.dailyActivityRepository
        )
    )
    val searchViewModel: SearchViewModel = viewModel(
        factory = SearchViewModel.Factory(app.applicationContext, app.foodRepository, app.diaryRepository)
    )
    val profileViewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModel.Factory(
            app.applicationContext,
            app.userPreferencesRepository,
            app.dailyActivityRepository
        )
    )
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(
            userPreferencesRepository = app.userPreferencesRepository,
            appUpdateChecker = app.appUpdateRepository,
            updateInstaller = app.apkUpdateInstaller,
            driveBackupService = app.driveBackupService,
            backupRepository = app.backupRepository,
            backupDocumentService = app.backupDocumentService,
            appVersionName = BuildConfig.VERSION_NAME,
            appVersionCode = BuildConfig.VERSION_CODE,
            onReminderChanged = { enabled, hour, minute ->
                if (enabled) {
                    MealReminderScheduler.scheduleDaily(app, hour, minute)
                } else {
                    MealReminderScheduler.cancel(app)
                }
            },
            onAutoBackupChanged = { enabled ->
                if (enabled) {
                    app.backupScheduler.scheduleWeeklyBackup()
                } else {
                    app.backupScheduler.cancelWeeklyBackup()
                }
            }
        )
    )
    val dashboardState by dashboardViewModel.uiState.collectAsState()

    val darkTheme = when (userPreferences?.themeMode ?: AppThemeMode.SYSTEM) {
        AppThemeMode.SYSTEM -> null
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    LaunchedEffect(dashboardState.selectedDate) {
        searchViewModel.setSelectedDate(dashboardState.selectedDate)
    }

    LaunchedEffect(launchAction, navController) {
        when (launchAction) {
            WidgetLaunchAction.NONE -> Unit
            WidgetLaunchAction.SEARCH_FOCUS -> {
                focusSearchOnStart = true
                openQuickAddOnStart = false
                openBarcodeScannerOnStart = false
                navController.navigateToBottomDestination(AppRoute.SEARCH)
                onLaunchActionHandled()
            }

            WidgetLaunchAction.BARCODE_SCANNER -> {
                openBarcodeScannerOnStart = true
                openQuickAddOnStart = false
                focusSearchOnStart = false
                navController.navigateToBottomDestination(AppRoute.SEARCH)
                onLaunchActionHandled()
            }
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isOnHome = currentRoute == AppRoute.HOME

    BackHandler(enabled = isOnHome && !showExitDialog) {
        showExitDialog = true
    }

    val density = LocalDensity.current
    val isKeyboardOpen = WindowInsets.ime.getBottom(density) > 0
    val resolvedDarkTheme = darkTheme ?: isSystemInDarkTheme()

    SyncSystemBarsWithTheme(darkTheme = resolvedDarkTheme)

    NozioTheme(darkTheme = resolvedDarkTheme) {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (!isKeyboardOpen) {
                    NavigationBar(
                        containerColor = MaterialTheme.nozioColors.baseBgElevated,
                        tonalElevation = 0.dp
                    ) {
                        AppDestination.entries.forEach { destination ->
                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        destination.icon,
                                        contentDescription = destination.label
                                    )
                                },
                                label = { Text(destination.label) },
                                selected = destination.matches(currentRoute),
                                onClick = {
                                    navController.navigateToBottomDestination(destination.route)
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = AppRoute.HOME,
                modifier = Modifier.padding(innerPadding),
                enterTransition = {
                    val forward = transitionDirection(targetState.destination.route, initialState.destination.route) >= 0
                    sharedAxisEnter(forward)
                },
                exitTransition = {
                    val forward = transitionDirection(targetState.destination.route, initialState.destination.route) >= 0
                    sharedAxisExit(forward)
                },
                popEnterTransition = {
                    val forward = transitionDirection(targetState.destination.route, initialState.destination.route) >= 0
                    sharedAxisEnter(!forward)
                },
                popExitTransition = {
                    val forward = transitionDirection(targetState.destination.route, initialState.destination.route) >= 0
                    sharedAxisExit(!forward)
                }
            ) {
                composable(AppRoute.HOME) {
                    DashboardScreen(
                        viewModel = dashboardViewModel,
                        onAddFood = { mealType ->
                            searchViewModel.setSelectedDate(dashboardState.selectedDate)
                            preselectedMealType = mealType
                            navController.navigateToBottomDestination(AppRoute.SEARCH)
                        }
                    )
                }

                composable(AppRoute.SEARCH) {
                    SearchScreen(
                        viewModel = searchViewModel,
                        preselectedMealType = preselectedMealType,
                        openQuickAddOnStart = openQuickAddOnStart,
                        openBarcodeScannerOnStart = openBarcodeScannerOnStart,
                        focusSearchOnStart = focusSearchOnStart,
                        onQuickAddOpened = { openQuickAddOnStart = false },
                        onBarcodeScannerOpened = { openBarcodeScannerOnStart = false },
                        onSearchFocused = { focusSearchOnStart = false }
                    )
                }

                composable(AppRoute.PROFILE) {
                    ProfileScreen(
                        viewModel = profileViewModel,
                        onEditGoals = { navController.navigate(AppRoute.PROFILE_EDIT) },
                        onOpenLegalInfo = { navController.navigate(AppRoute.PROFILE_LEGAL) }
                    )
                }

                composable(AppRoute.PROFILE_EDIT) {
                    ProfileEditGoalsScreen(
                        viewModel = profileViewModel,
                        onBack = { navController.navigateUp() }
                    )
                }

                composable(AppRoute.PROFILE_LEGAL) {
                    LegalInfoScreen(onBack = { navController.navigateUp() })
                }

                composable(AppRoute.SETTINGS_MAIN) {
                    SettingsScreen(
                        viewModel = settingsViewModel,
                        section = SettingsSection.MAIN,
                        onNavigateToReminder = { navController.navigate(AppRoute.SETTINGS_REMINDER) },
                        onNavigateToBackup = { navController.navigate(AppRoute.SETTINGS_BACKUP) }
                    )
                }

                composable(AppRoute.SETTINGS_REMINDER) {
                    SettingsScreen(
                        viewModel = settingsViewModel,
                        section = SettingsSection.REMINDER,
                        onBack = { navController.navigateUp() }
                    )
                }

                composable(AppRoute.SETTINGS_BACKUP) {
                    SettingsScreen(
                        viewModel = settingsViewModel,
                        section = SettingsSection.BACKUP,
                        onBack = { navController.navigateUp() }
                    )
                }
            }
        }

        if (showExitDialog) {
            val activity = LocalContext.current.findActivity()
            AlertDialog(
                onDismissRequest = { showExitDialog = false },
                title = { Text("App verlassen?") },
                text = { Text("Moechtest du Nozio wirklich schliessen?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showExitDialog = false
                            activity?.finish()
                        }
                    ) {
                        Text("Ja")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExitDialog = false }) {
                        Text("Nein")
                    }
                }
            )
        }
    }
}

private fun transitionDirection(targetRoute: String?, initialRoute: String?): Int {
    return routeRank(targetRoute) - routeRank(initialRoute)
}

private fun routeRank(route: String?): Int {
    return when (route) {
        AppRoute.HOME -> 0
        AppRoute.SEARCH -> 1
        AppRoute.PROFILE,
        AppRoute.PROFILE_EDIT,
        AppRoute.PROFILE_LEGAL -> 2

        AppRoute.SETTINGS_MAIN,
        AppRoute.SETTINGS_REMINDER,
        AppRoute.SETTINGS_BACKUP -> 3

        else -> 0
    }
}

private fun sharedAxisEnter(forward: Boolean) = slideInHorizontally(
    animationSpec = tween(280),
    initialOffsetX = { fullWidth ->
        val distance = (fullWidth * 0.12f).toInt()
        if (forward) distance else -distance
    }
) + fadeIn(animationSpec = tween(280))

private fun sharedAxisExit(forward: Boolean) = slideOutHorizontally(
    animationSpec = tween(280),
    targetOffsetX = { fullWidth ->
        val distance = (fullWidth * 0.12f).toInt()
        if (forward) -distance else distance
    }
) + fadeOut(animationSpec = tween(220))

private fun NavHostController.navigateToBottomDestination(route: String) {
    navigate(
        route,
        navOptions {
            launchSingleTop = true
            restoreState = true
            popUpTo(graph.findStartDestination().id) {
                saveState = true
            }
        }
    )
}

@Composable
private fun SyncSystemBarsWithTheme(darkTheme: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    val activity = view.context.findActivity() ?: return

    SideEffect {
        val insetsController = WindowCompat.getInsetsController(activity.window, view)
        insetsController.isAppearanceLightStatusBars = !darkTheme
        insetsController.isAppearanceLightNavigationBars = !darkTheme
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

enum class AppDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    HOME(AppRoute.HOME, "Home", Icons.Default.Home),
    SEARCH(AppRoute.SEARCH, "Suche", Icons.Default.Search),
    PROFILE(AppRoute.PROFILE, "Profil", Icons.Default.Person),
    SETTINGS(AppRoute.SETTINGS_MAIN, "Einstellungen", Icons.Default.Settings);

    fun matches(currentRoute: String?): Boolean {
        return when (this) {
            HOME -> currentRoute == AppRoute.HOME
            SEARCH -> currentRoute == AppRoute.SEARCH
            PROFILE -> currentRoute == AppRoute.PROFILE ||
                currentRoute == AppRoute.PROFILE_EDIT ||
                currentRoute == AppRoute.PROFILE_LEGAL
            SETTINGS -> currentRoute?.startsWith("settings/") == true
        }
    }
}
