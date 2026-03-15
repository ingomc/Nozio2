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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.TransformOrigin
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
import de.ingomc.nozio.ui.meals.MealsIngredientPickerTarget
import de.ingomc.nozio.ui.meals.MealsScreen
import de.ingomc.nozio.ui.meals.MealsViewModel
import de.ingomc.nozio.ui.profile.LegalInfoScreen
import de.ingomc.nozio.ui.profile.ProfileEditGoalsScreen
import de.ingomc.nozio.ui.profile.ProfileScreen
import de.ingomc.nozio.ui.profile.ProfileViewModel
import de.ingomc.nozio.ui.search.AddConfirmationBanner
import de.ingomc.nozio.ui.search.AddConfirmationState
import de.ingomc.nozio.ui.search.SearchScreen
import de.ingomc.nozio.ui.search.SearchMode
import de.ingomc.nozio.ui.search.SearchViewModel
import de.ingomc.nozio.ui.settings.SettingsScreen
import de.ingomc.nozio.ui.settings.SettingsSection
import de.ingomc.nozio.ui.settings.SettingsViewModel
import de.ingomc.nozio.ui.supplements.SupplementsEditScreen
import de.ingomc.nozio.ui.supplements.SupplementsEditViewModel
import de.ingomc.nozio.ui.theme.ExpressiveMotion
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
    const val MEALS = "meals"
    const val SEARCH = "search"
    const val SEARCH_PICK_EDITOR = "search/pickIngredient/editor"
    const val SEARCH_PICK_TRACKER = "search/pickIngredient/tracker"
    const val PROFILE = "profile"
    const val PROFILE_EDIT = "profile/edit"
    const val SETTINGS_LEGAL = "settings/legal"
    const val SETTINGS_MAIN = "settings/main"
    const val SETTINGS_REMINDER = "settings/reminder"
    const val SETTINGS_BACKUP = "settings/backup"
    const val SUPPLEMENTS_EDIT = "supplements/edit"
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
            app.dailyActivityRepository,
            app.supplementRepository
        )
    )
    val searchViewModel: SearchViewModel = viewModel(
        factory = SearchViewModel.Factory(app.applicationContext, app.foodRepository, app.diaryRepository)
    )
    val ingredientPickerSearchViewModel: SearchViewModel = viewModel(
        key = "ingredient_picker_search_vm",
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
    val supplementsEditViewModel: SupplementsEditViewModel = viewModel(
        factory = SupplementsEditViewModel.Factory(
            supplementRepository = app.supplementRepository
        )
    )
    val mealsViewModel: MealsViewModel = viewModel(
        factory = MealsViewModel.Factory(
            appContext = app.applicationContext,
            mealTemplateRepository = app.mealTemplateRepository,
            foodRepository = app.foodRepository
        )
    )
    val dashboardState by dashboardViewModel.uiState.collectAsState()
    val searchState by searchViewModel.uiState.collectAsState()
    val addConfirmationProgress = remember { Animatable(0f) }
    val addBannerVisibilityState = remember { MutableTransitionState(false) }
    var displayedAddConfirmation by remember { mutableStateOf<AddConfirmationState?>(null) }

    val darkTheme = when (userPreferences?.themeMode ?: AppThemeMode.SYSTEM) {
        AppThemeMode.SYSTEM -> null
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    LaunchedEffect(dashboardState.selectedDate) {
        searchViewModel.setSelectedDate(dashboardState.selectedDate)
        mealsViewModel.setSelectedDate(dashboardState.selectedDate)
    }

    LaunchedEffect(searchState.activeAddConfirmation?.bannerId) {
        val confirmation = searchState.activeAddConfirmation
        if (confirmation == null) {
            addConfirmationProgress.snapTo(0f)
        } else {
            addConfirmationProgress.snapTo(1f)
            addConfirmationProgress.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 3000, easing = LinearEasing)
            )
            searchViewModel.dismissAddConfirmation()
        }
    }

    LaunchedEffect(searchState.activeAddConfirmation) {
        val confirmation = searchState.activeAddConfirmation
        if (confirmation != null) {
            displayedAddConfirmation = confirmation
            addBannerVisibilityState.targetState = true
        } else {
            addBannerVisibilityState.targetState = false
        }
    }

    LaunchedEffect(
        addBannerVisibilityState.currentState,
        addBannerVisibilityState.targetState,
        addBannerVisibilityState.isIdle,
        searchState.activeAddConfirmation
    ) {
        if (
            searchState.activeAddConfirmation == null &&
            addBannerVisibilityState.isIdle &&
            !addBannerVisibilityState.currentState &&
            !addBannerVisibilityState.targetState
        ) {
            displayedAddConfirmation = null
        }
    }

    LaunchedEffect(launchAction, navController) {
        when (launchAction) {
            WidgetLaunchAction.NONE -> Unit
            WidgetLaunchAction.SEARCH_FOCUS -> {
                focusSearchOnStart = true
                openQuickAddOnStart = false
                openBarcodeScannerOnStart = false
                navController.navigate(AppRoute.SEARCH)
                onLaunchActionHandled()
            }

            WidgetLaunchAction.BARCODE_SCANNER -> {
                openBarcodeScannerOnStart = true
                openQuickAddOnStart = false
                focusSearchOnStart = false
                navController.navigate(AppRoute.SEARCH)
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
    val hideBottomNavigationRoutes = remember {
        setOf(
            AppRoute.SEARCH,
            AppRoute.SEARCH_PICK_EDITOR,
            AppRoute.SEARCH_PICK_TRACKER
        )
    }
    val shouldShowBottomNavigation = !isKeyboardOpen && currentRoute !in hideBottomNavigationRoutes
    val resolvedDarkTheme = darkTheme ?: isSystemInDarkTheme()

    SyncSystemBarsWithTheme(darkTheme = resolvedDarkTheme)

    NozioTheme(darkTheme = resolvedDarkTheme) {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (shouldShowBottomNavigation) {
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
                                    indicatorColor = MaterialTheme.nozioColors.emphasisContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                NavHost(
                    navController = navController,
                    startDestination = AppRoute.HOME,
                    modifier = Modifier.fillMaxSize(),
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
                                navController.navigate(AppRoute.SEARCH)
                            },
                            onEditSupplements = { navController.navigate(AppRoute.SUPPLEMENTS_EDIT) }
                        )
                    }

                    composable(AppRoute.MEALS) {
                        MealsScreen(
                            viewModel = mealsViewModel,
                            onOpenIngredientPicker = { target ->
                                val route = when (target) {
                                    MealsIngredientPickerTarget.EDITOR -> AppRoute.SEARCH_PICK_EDITOR
                                    MealsIngredientPickerTarget.TRACKER -> AppRoute.SEARCH_PICK_TRACKER
                                }
                                navController.navigate(route)
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

                    composable(AppRoute.SEARCH_PICK_EDITOR) {
                        SearchScreen(
                            viewModel = ingredientPickerSearchViewModel,
                            preselectedMealType = null,
                            mode = SearchMode.INGREDIENT_PICKER,
                            onFoodPicked = { food ->
                                mealsViewModel.addPickedIngredient(food, MealsIngredientPickerTarget.EDITOR)
                                navController.navigateUp()
                            }
                        )
                    }

                    composable(AppRoute.SEARCH_PICK_TRACKER) {
                        SearchScreen(
                            viewModel = ingredientPickerSearchViewModel,
                            preselectedMealType = null,
                            mode = SearchMode.INGREDIENT_PICKER,
                            onFoodPicked = { food ->
                                mealsViewModel.addPickedIngredient(food, MealsIngredientPickerTarget.TRACKER)
                                navController.navigateUp()
                            }
                        )
                    }

                    composable(AppRoute.PROFILE) {
                        ProfileScreen(
                            viewModel = profileViewModel,
                            onEditGoals = { navController.navigate(AppRoute.PROFILE_EDIT) }
                        )
                    }

                    composable(AppRoute.PROFILE_EDIT) {
                        ProfileEditGoalsScreen(
                            viewModel = profileViewModel,
                            onBack = { navController.navigateUp() }
                        )
                    }

                    composable(AppRoute.SETTINGS_LEGAL) {
                        LegalInfoScreen(onBack = { navController.navigateUp() })
                    }

                    composable(AppRoute.SETTINGS_MAIN) {
                        SettingsScreen(
                            viewModel = settingsViewModel,
                            section = SettingsSection.MAIN,
                            onNavigateToReminder = { navController.navigate(AppRoute.SETTINGS_REMINDER) },
                            onNavigateToBackup = { navController.navigate(AppRoute.SETTINGS_BACKUP) },
                            onNavigateToLegalInfo = { navController.navigate(AppRoute.SETTINGS_LEGAL) }
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

                    composable(AppRoute.SUPPLEMENTS_EDIT) {
                        SupplementsEditScreen(
                            viewModel = supplementsEditViewModel,
                            onBack = { navController.navigateUp() }
                        )
                    }
                }

                AnimatedVisibility(
                    visibleState = addBannerVisibilityState,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 8.dp, start = 16.dp, end = 16.dp),
                    enter = fadeIn(
                        animationSpec = tween(
                            durationMillis = ExpressiveMotion.DurationShort,
                            easing = ExpressiveMotion.Standard
                        )
                    ) + slideInVertically(
                        animationSpec = spring(
                            dampingRatio = 0.8f,
                            stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                        ),
                        initialOffsetY = { -it }
                    ) + scaleIn(
                        initialScale = 0.96f,
                        transformOrigin = TransformOrigin(0.5f, 0f),
                        animationSpec = spring(
                            dampingRatio = 0.88f,
                            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                        )
                    ),
                    exit = fadeOut(
                        animationSpec = tween(
                            durationMillis = ExpressiveMotion.DurationShort,
                            easing = ExpressiveMotion.Emphasized
                        )
                    ) + slideOutVertically(
                        animationSpec = tween(
                            durationMillis = ExpressiveMotion.DurationMedium,
                            easing = ExpressiveMotion.Emphasized
                        ),
                        targetOffsetY = { -it / 2 }
                    ) + scaleOut(
                        targetScale = 0.98f,
                        transformOrigin = TransformOrigin(0.5f, 0f),
                        animationSpec = tween(
                            durationMillis = ExpressiveMotion.DurationShort,
                            easing = ExpressiveMotion.Standard
                        )
                    )
                ) {
                    displayedAddConfirmation?.let { confirmation ->
                        AddConfirmationBanner(
                            confirmation = confirmation,
                            progress = addConfirmationProgress.value,
                            onUndo = searchViewModel::undoLastAddedFood,
                            onConfirm = searchViewModel::dismissAddConfirmation
                        )
                    }
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
        AppRoute.SUPPLEMENTS_EDIT -> 1
        AppRoute.MEALS -> 2
        AppRoute.SEARCH,
        AppRoute.SEARCH_PICK_EDITOR,
        AppRoute.SEARCH_PICK_TRACKER -> 3
        AppRoute.PROFILE,
        AppRoute.PROFILE_EDIT -> 4

        AppRoute.SETTINGS_MAIN,
        AppRoute.SETTINGS_REMINDER,
        AppRoute.SETTINGS_BACKUP,
        AppRoute.SETTINGS_LEGAL -> 5

        else -> 0
    }
}

private fun sharedAxisEnter(forward: Boolean) = slideInHorizontally(
    animationSpec = tween(ExpressiveMotion.DurationLong, easing = ExpressiveMotion.Emphasized),
    initialOffsetX = { fullWidth ->
        val distance = (fullWidth * 0.16f).toInt()
        if (forward) distance else -distance
    }
) + fadeIn(animationSpec = tween(ExpressiveMotion.DurationMedium, easing = ExpressiveMotion.Standard))

private fun sharedAxisExit(forward: Boolean) = slideOutHorizontally(
    animationSpec = tween(ExpressiveMotion.DurationLong, easing = ExpressiveMotion.Emphasized),
    targetOffsetX = { fullWidth ->
        val distance = (fullWidth * 0.16f).toInt()
        if (forward) -distance else distance
    }
) + fadeOut(animationSpec = tween(ExpressiveMotion.DurationMedium, easing = ExpressiveMotion.Standard))

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
    MEALS(AppRoute.MEALS, "Meals", Icons.Default.Restaurant),
    PROFILE(AppRoute.PROFILE, "Profil", Icons.Default.Person),
    SETTINGS(AppRoute.SETTINGS_MAIN, "Einstellungen", Icons.Default.Settings);

    fun matches(currentRoute: String?): Boolean {
        return when (this) {
            HOME -> currentRoute == AppRoute.HOME || currentRoute == AppRoute.SUPPLEMENTS_EDIT
            MEALS -> currentRoute == AppRoute.MEALS
            PROFILE -> currentRoute == AppRoute.PROFILE ||
                currentRoute == AppRoute.PROFILE_EDIT
            SETTINGS -> currentRoute?.startsWith("settings/") == true
        }
    }
}
