package de.ingomc.nozio

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.lifecycle.viewmodel.compose.viewModel
import de.ingomc.nozio.data.local.MealType
import de.ingomc.nozio.ui.dashboard.DashboardScreen
import de.ingomc.nozio.ui.dashboard.DashboardViewModel
import de.ingomc.nozio.ui.profile.ProfileScreen
import de.ingomc.nozio.ui.profile.ProfileViewModel
import de.ingomc.nozio.ui.search.SearchScreen
import de.ingomc.nozio.ui.search.SearchViewModel
import de.ingomc.nozio.ui.theme.NozioTheme
import kotlinx.coroutines.launch

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
    val context = LocalContext.current
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    var preselectedMealType by rememberSaveable { mutableStateOf<MealType?>(null) }

    val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class)
    )

    val dashboardViewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModel.Factory(app.diaryRepository, app.userPreferencesRepository, app.healthConnectRepository)
    )
    val searchViewModel: SearchViewModel = viewModel(
        factory = SearchViewModel.Factory(app.foodRepository, app.diaryRepository)
    )
    val profileViewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModel.Factory(app.userPreferencesRepository)
    )

    var grantedPermissions by remember { mutableStateOf(emptySet<String>()) }
    val scope = rememberCoroutineScope()
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        grantedPermissions = granted
        dashboardViewModel.logHealthEvent("Permission-Result: ${granted.size}/${permissions.size} Berechtigungen erteilt")
    }

    LaunchedEffect(grantedPermissions) {
        if (grantedPermissions.containsAll(permissions)) {
            dashboardViewModel.logHealthEvent("Alle Berechtigungen vorhanden. Aktualisiere Health-Daten.")
            dashboardViewModel.refreshHealthData()
        }
    }

    val connectHealth: () -> Unit = {
        scope.launch {
            dashboardViewModel.logHealthEvent("Starte Health-Connect-Verbindung...")
            val sdkStatus = HealthConnectClient.getSdkStatus(context)

            when (sdkStatus) {
                HealthConnectClient.SDK_AVAILABLE -> {
                    dashboardViewModel.logHealthEvent("Health Connect SDK verfügbar.")
                    val healthConnectClient = HealthConnectClient.getOrCreate(context)
                    val granted = healthConnectClient.permissionController.getGrantedPermissions()
                    grantedPermissions = granted
                    dashboardViewModel.logHealthEvent("Aktuelle Berechtigungen: ${granted.size}/${permissions.size}")
                    if (!granted.containsAll(permissions)) {
                        dashboardViewModel.logHealthEvent("Öffne Berechtigungsdialog...")
                        try {
                            permissionLauncher.launch(permissions)
                        } catch (e: Exception) {
                            dashboardViewModel.logHealthEvent("Permission-Dialog Fehler: ${e.javaClass.simpleName}: ${e.message ?: "unbekannt"}")
                            dashboardViewModel.logHealthEvent("Öffne stattdessen Health-Connect-Einstellungen...")
                            try {
                                context.startActivity(Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS))
                            } catch (settingsError: ActivityNotFoundException) {
                                dashboardViewModel.logHealthEvent("Health-Connect-Einstellungen konnten nicht geöffnet werden.")
                            }
                        }
                    } else {
                        dashboardViewModel.refreshHealthData()
                    }
                }

                HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
                    dashboardViewModel.logHealthEvent("Health Connect muss installiert/aktualisiert werden.")
                    try {
                        context.startActivity(Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS))
                    } catch (_: ActivityNotFoundException) {
                        dashboardViewModel.logHealthEvent("Health-Connect-Einstellungen konnten nicht geöffnet werden.")
                    }
                }

                else -> {
                    dashboardViewModel.logHealthEvent("Health Connect ist auf diesem Gerät nicht verfügbar (Status $sdkStatus).")
                }
            }
        }
        Unit
    }

    val openHealthSettings: () -> Unit = {
        dashboardViewModel.logHealthEvent("Oeffne Health-Connect-Einstellungen...")
        try {
            context.startActivity(Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS))
        } catch (_: ActivityNotFoundException) {
            dashboardViewModel.logHealthEvent("Health-Connect-Einstellungen konnten nicht geoeffnet werden.")
        }
    }

    LaunchedEffect(Unit) {
        val sdkStatus = HealthConnectClient.getSdkStatus(context)
        dashboardViewModel.logHealthEvent("Initialer SDK-Status: $sdkStatus")
        dashboardViewModel.logHealthEvent("Geraet: ${Build.MANUFACTURER} ${Build.MODEL}")
        dashboardViewModel.logHealthEvent("Kein Auto-Connect beim Start. Bitte 'Health Connect verbinden' nutzen.")
    }

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
                onConnectHealth = connectHealth,
                onOpenHealthSettings = openHealthSettings,
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
