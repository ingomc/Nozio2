package de.ingomc.nozio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.MaterialTheme
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
import androidx.lifecycle.viewmodel.compose.viewModel
import de.ingomc.nozio.data.local.MealType
import de.ingomc.nozio.ui.dashboard.DashboardScreen
import de.ingomc.nozio.ui.dashboard.DashboardViewModel
import de.ingomc.nozio.ui.profile.ProfileScreen
import de.ingomc.nozio.ui.profile.ProfileViewModel
import de.ingomc.nozio.ui.search.SearchScreen
import de.ingomc.nozio.ui.search.SearchViewModel
import de.ingomc.nozio.ui.theme.NozioTheme
import de.ingomc.nozio.ui.theme.nozioColors

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NozioTheme {
                NozioApp()
            }
        }
    }
}

@Composable
fun NozioApp() {
    val app = LocalContext.current.applicationContext as NozioApplication
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    var preselectedMealType by rememberSaveable { mutableStateOf<MealType?>(null) }

    val dashboardViewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModel.Factory(
            app.diaryRepository,
            app.userPreferencesRepository,
            app.dailyActivityRepository
        )
    )
    val searchViewModel: SearchViewModel = viewModel(
        factory = SearchViewModel.Factory(app.foodRepository, app.diaryRepository)
    )
    val profileViewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModel.Factory(app.userPreferencesRepository)
    )
    val dashboardState by dashboardViewModel.uiState.collectAsState()

    LaunchedEffect(dashboardState.selectedDate) {
        searchViewModel.setSelectedDate(dashboardState.selectedDate)
    }

    val density = LocalDensity.current
    val isKeyboardOpen = WindowInsets.ime.getBottom(density) > 0

    Scaffold(
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
                                if (it != AppDestinations.SEARCH) {
                                    preselectedMealType = null
                                }
                                currentDestination = it
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
    ) {
        when (currentDestination) {
            AppDestinations.HOME -> DashboardScreen(
                viewModel = dashboardViewModel,
                modifier = Modifier
                    .padding(it)
                    .statusBarsPadding(),
                onAddFood = { mealType ->
                    searchViewModel.setSelectedDate(dashboardState.selectedDate)
                    preselectedMealType = mealType
                    currentDestination = AppDestinations.SEARCH
                }
            )

            AppDestinations.SEARCH -> SearchScreen(
                viewModel = searchViewModel,
                preselectedMealType = preselectedMealType,
                modifier = Modifier.padding(it)
            )

            AppDestinations.PROFILE -> ProfileScreen(
                viewModel = profileViewModel,
                modifier = Modifier
                    .padding(it)
                    .statusBarsPadding()
            )
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
}
