package de.ingomc.nozio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import de.ingomc.nozio.data.local.MealType
import de.ingomc.nozio.data.repository.AppThemeMode
import de.ingomc.nozio.notifications.MealReminderScheduler
import de.ingomc.nozio.ui.dashboard.DashboardScreen
import de.ingomc.nozio.ui.dashboard.DashboardViewModel
import de.ingomc.nozio.ui.profile.LegalInfoScreen
import de.ingomc.nozio.ui.profile.ProfileScreen
import de.ingomc.nozio.ui.profile.ProfileViewModel
import de.ingomc.nozio.ui.search.SearchScreen
import de.ingomc.nozio.ui.search.SearchViewModel
import de.ingomc.nozio.ui.settings.SettingsScreen
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

@Composable
fun NozioApp(
    launchAction: WidgetLaunchAction = WidgetLaunchAction.NONE,
    onLaunchActionHandled: () -> Unit = {}
) {
    val app = LocalContext.current.applicationContext as NozioApplication
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    var preselectedMealType by rememberSaveable { mutableStateOf<MealType?>(null) }
    var showLegalInfo by rememberSaveable { mutableStateOf(false) }
    var openQuickAddOnStart by rememberSaveable { mutableStateOf(false) }
    var openBarcodeScannerOnStart by rememberSaveable { mutableStateOf(false) }
    var focusSearchOnStart by rememberSaveable { mutableStateOf(false) }
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
        factory = SettingsViewModel.Factory(app.userPreferencesRepository) { enabled, hour, minute ->
            if (enabled) {
                MealReminderScheduler.scheduleDaily(app, hour, minute)
            } else {
                MealReminderScheduler.cancel(app)
            }
        }
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
    LaunchedEffect(launchAction) {
        when (launchAction) {
            WidgetLaunchAction.NONE -> Unit
            WidgetLaunchAction.SEARCH_FOCUS -> {
                currentDestination = AppDestinations.SEARCH
                showLegalInfo = false
                focusSearchOnStart = true
                openQuickAddOnStart = false
                openBarcodeScannerOnStart = false
                onLaunchActionHandled()
            }

            WidgetLaunchAction.BARCODE_SCANNER -> {
                currentDestination = AppDestinations.SEARCH
                showLegalInfo = false
                openBarcodeScannerOnStart = true
                openQuickAddOnStart = false
                focusSearchOnStart = false
                onLaunchActionHandled()
            }
        }
    }

    val density = LocalDensity.current
    val isKeyboardOpen = WindowInsets.ime.getBottom(density) > 0

    NozioTheme(darkTheme = darkTheme ?: isSystemInDarkTheme()) {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (!isKeyboardOpen) {
                    NavigationBar(
                        containerColor = MaterialTheme.nozioColors.baseBgElevated,
                        tonalElevation = 0.dp
                    ) {
                        AppDestinations.entries.forEach {
                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        it.icon,
                                        contentDescription = it.label
                                    )
                                },
                                label = { Text(it.label) },
                                selected = it == currentDestination,
                                onClick = {
                                    currentDestination = it
                                    showLegalInfo = false
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
            AnimatedContent(
                targetState = currentDestination,
                transitionSpec = {
                    val forward = targetState.ordinal > initialState.ordinal
                    val distanceFraction = 0.12f
                    ContentTransform(
                        targetContentEnter =
                            slideInHorizontally(
                                animationSpec = tween(280),
                                initialOffsetX = { fullWidth ->
                                    val distance = (fullWidth * distanceFraction).toInt()
                                    if (forward) distance else -distance
                                }
                            ) + fadeIn(animationSpec = tween(280)),
                        initialContentExit =
                            slideOutHorizontally(
                                animationSpec = tween(280),
                                targetOffsetX = { fullWidth ->
                                    val distance = (fullWidth * distanceFraction).toInt()
                                    if (forward) -distance else distance
                                }
                            ) + fadeOut(animationSpec = tween(220))
                    )
                },
                label = "bottomNavSharedAxisX"
            ) { destination ->
                if (showLegalInfo) {
                    LegalInfoScreen(
                        modifier = Modifier.padding(innerPadding),
                        onBack = { showLegalInfo = false }
                    )
                } else {
                    when (destination) {
                        AppDestinations.HOME -> DashboardScreen(
                            viewModel = dashboardViewModel,
                            modifier = Modifier.padding(innerPadding),
                            onAddFood = { mealType ->
                                searchViewModel.setSelectedDate(dashboardState.selectedDate)
                                preselectedMealType = mealType
                                currentDestination = AppDestinations.SEARCH
                            }
                        )

                        AppDestinations.SEARCH -> SearchScreen(
                            viewModel = searchViewModel,
                            preselectedMealType = preselectedMealType,
                            openQuickAddOnStart = openQuickAddOnStart,
                            openBarcodeScannerOnStart = openBarcodeScannerOnStart,
                            focusSearchOnStart = focusSearchOnStart,
                            onQuickAddOpened = { openQuickAddOnStart = false },
                            onBarcodeScannerOpened = { openBarcodeScannerOnStart = false },
                            onSearchFocused = { focusSearchOnStart = false },
                            modifier = Modifier.padding(innerPadding)
                        )

                        AppDestinations.PROFILE -> ProfileScreen(
                            viewModel = profileViewModel,
                            modifier = Modifier.padding(innerPadding),
                            onOpenLegalInfo = { showLegalInfo = true }
                        )

                        AppDestinations.SETTINGS -> SettingsScreen(
                            viewModel = settingsViewModel,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    HOME("Home", Icons.Default.Home),
    SEARCH("Suche", Icons.Default.Search),
    PROFILE("Profil", Icons.Default.Person),
    SETTINGS("Einstellungen", Icons.Default.Settings),
}
