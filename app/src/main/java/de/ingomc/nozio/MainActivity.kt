package de.ingomc.nozio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import de.ingomc.nozio.data.local.MealType
import de.ingomc.nozio.ui.dashboard.DashboardScreen
import de.ingomc.nozio.ui.dashboard.DashboardViewModel
import de.ingomc.nozio.ui.profile.ProfileScreen
import de.ingomc.nozio.ui.profile.ProfileViewModel
import de.ingomc.nozio.ui.search.SearchScreen
import de.ingomc.nozio.ui.search.SearchViewModel
import de.ingomc.nozio.ui.theme.NozioTheme

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
        factory = DashboardViewModel.Factory(app.diaryRepository, app.userPreferencesRepository)
    )
    val searchViewModel: SearchViewModel = viewModel(
        factory = SearchViewModel.Factory(app.foodRepository, app.diaryRepository)
    )
    val profileViewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModel.Factory(app.userPreferencesRepository)
    )

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
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
                    }
                )
            }
        }
    ) {
        when (currentDestination) {
            AppDestinations.HOME -> DashboardScreen(
                viewModel = dashboardViewModel,
                modifier = Modifier.statusBarsPadding(),
                onAddFood = { mealType ->
                    preselectedMealType = mealType
                    currentDestination = AppDestinations.SEARCH
                }
            )
            AppDestinations.SEARCH -> SearchScreen(
                viewModel = searchViewModel,
                preselectedMealType = preselectedMealType,
                modifier = Modifier
            )
            AppDestinations.PROFILE -> ProfileScreen(
                viewModel = profileViewModel,
                modifier = Modifier.statusBarsPadding()
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
