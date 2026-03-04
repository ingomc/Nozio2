package de.ingomc.nozio.ui.search

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import de.ingomc.nozio.data.local.FoodItem
import de.ingomc.nozio.data.local.MealType
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    preselectedMealType: MealType?,
    modifier: Modifier = Modifier,
    openQuickAddOnStart: Boolean = false,
    openBarcodeScannerOnStart: Boolean = false,
    focusSearchOnStart: Boolean = false,
    onQuickAddOpened: () -> Unit = {},
    onBarcodeScannerOpened: () -> Unit = {},
    onSearchFocused: () -> Unit = {}
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val searchFocusRequester = remember { FocusRequester() }
    val addConfirmationProgress = remember { Animatable(0f) }
    val addBannerVisibilityState = remember { MutableTransitionState(false) }
    var displayedAddConfirmation by remember { mutableStateOf<AddConfirmationState?>(null) }
    var showScannerSheet by remember { mutableStateOf(false) }
    var searchContainerHeightPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    val listBottomPadding = with(density) { searchContainerHeightPx.toDp() + 8.dp }
    val showingSuggestions = state.query.length < 3
    val foodsToShow = if (showingSuggestions) state.recentSuggestions else state.results
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showScannerSheet = true
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Kamera-Berechtigung benötigt")
            }
        }
    }
    val launchBarcodeScanner: () -> Unit = {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            showScannerSheet = true
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(openQuickAddOnStart) {
        if (openQuickAddOnStart) {
            viewModel.showQuickAddSheet()
            onQuickAddOpened()
        }
    }

    LaunchedEffect(openBarcodeScannerOnStart) {
        if (openBarcodeScannerOnStart) {
            launchBarcodeScanner()
            onBarcodeScannerOpened()
        }
    }

    LaunchedEffect(focusSearchOnStart) {
        if (focusSearchOnStart) {
            searchFocusRequester.requestFocus()
            keyboardController?.show()
            onSearchFocused()
        }
    }

    LaunchedEffect(state.activeAddConfirmation?.bannerId) {
        val confirmation = state.activeAddConfirmation
        if (confirmation == null) {
            addConfirmationProgress.snapTo(0f)
        } else {
            addConfirmationProgress.snapTo(1f)
            addConfirmationProgress.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 5000, easing = LinearEasing)
            )
            viewModel.dismissAddConfirmation()
        }
    }

    LaunchedEffect(state.activeAddConfirmation) {
        val confirmation = state.activeAddConfirmation
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
        state.activeAddConfirmation
    ) {
        if (
            state.activeAddConfirmation == null &&
            addBannerVisibilityState.isIdle &&
            !addBannerVisibilityState.currentState &&
            !addBannerVisibilityState.targetState
        ) {
            displayedAddConfirmation = null
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal
        )
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Loading
                if (state.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                // Error
                state.error?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                // Empty state
                if (!state.isLoading && state.results.isEmpty() && state.query.length >= 3 && state.error == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Keine Ergebnisse gefunden",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Hint when no query
                if (state.query.length < 3 && state.recentSuggestions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                text = "Suche nach Lebensmitteln",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Mindestens 3 Zeichen eingeben",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Results
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = listBottomPadding)
                ) {
                    if (state.query.length < 3) {
                        item {
                            QuickActionsCard(
                                onQuickAddClick = viewModel::showQuickAddSheet,
                                onCreateFoodClick = viewModel::showCreateCustomFoodSheet
                            )
                            HorizontalDivider()
                        }
                    }
                    if (showingSuggestions && foodsToShow.isNotEmpty()) {
                        item {
                            Text(
                                text = "Zuletzt hinzugefügt",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                    items(foodsToShow) { food ->
                        FoodSearchItem(
                            food = food,
                            onClick = { viewModel.selectFood(food) }
                        )
                        HorizontalDivider()
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .imePadding()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onSizeChanged { searchContainerHeightPx = it.height },
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        HorizontalDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = state.query,
                                onValueChange = viewModel::onQueryChange,
                                singleLine = true,
                                placeholder = { Text("Lebensmittel suchen...") },
                                leadingIcon = {
                                    Icon(Icons.Default.Search, contentDescription = "Suchen")
                                },
                                trailingIcon = {
                                    if (state.query.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.onQueryChange("") }) {
                                            Icon(Icons.Default.Close, contentDescription = "Suche leeren")
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .focusRequester(searchFocusRequester),
                                shape = RoundedCornerShape(28.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.outline,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    focusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    focusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )

                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape,
                                tonalElevation = 0.dp,
                                shadowElevation = 0.dp
                            ) {
                                IconButton(
                                    onClick = {
                                        launchBarcodeScanner()
                                    },
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Icon(
                                        Icons.Default.QrCodeScanner,
                                        contentDescription = "Barcode scannen",
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 72.dp)
            )

            AnimatedVisibility(
                visibleState = addBannerVisibilityState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 8.dp, start = 16.dp, end = 16.dp),
                enter = fadeIn(animationSpec = tween(durationMillis = 180)) +
                    slideInVertically(
                        animationSpec = spring(
                            dampingRatio = 0.8f,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        initialOffsetY = { -it }
                    ) +
                    scaleIn(
                        initialScale = 0.96f,
                        transformOrigin = TransformOrigin(0.5f, 0f),
                        animationSpec = spring(
                            dampingRatio = 0.88f,
                            stiffness = Spring.StiffnessMedium
                        )
                    ),
                exit = fadeOut(animationSpec = tween(durationMillis = 140)) +
                    slideOutVertically(
                        animationSpec = tween(
                            durationMillis = 180,
                            easing = FastOutLinearInEasing
                        ),
                        targetOffsetY = { -it / 2 }
                    ) +
                    scaleOut(
                        targetScale = 0.98f,
                        transformOrigin = TransformOrigin(0.5f, 0f),
                        animationSpec = tween(durationMillis = 140)
                    )
            ) {
                displayedAddConfirmation?.let { confirmation ->
                    AddConfirmationBanner(
                        confirmation = confirmation,
                        progress = addConfirmationProgress.value,
                        onUndo = viewModel::undoLastAddedFood
                    )
                }
            }
        }
    }

    if (showScannerSheet) {
        BarcodeScannerBottomSheet(
            onDismiss = { showScannerSheet = false },
            onBarcodeDetected = { barcode ->
                showScannerSheet = false
                viewModel.onBarcodeScanned(barcode)
            }
        )
    }

    if (state.showBarcodeResultsSheet && state.results.isNotEmpty()) {
        BarcodeResultsBottomSheet(
            results = state.results,
            maxPreviewItems = 4,
            onDismiss = viewModel::dismissBarcodeResultsSheet,
            onResultClick = { food -> viewModel.selectFood(food) },
            onShowAllResults = viewModel::dismissBarcodeResultsSheet
        )
    }

    // Bottom Sheet
    if (state.showBottomSheet && state.selectedFood != null) {
        AddFoodBottomSheet(
            food = state.selectedFood!!,
            preselectedMealType = preselectedMealType,
            onDismiss = viewModel::dismissBottomSheet,
            onAdd = { mealType, amount, amountLabel ->
                viewModel.addFood(mealType, amount, amountLabel)
            }
        )
    }

    if (state.showQuickAddSheet) {
        QuickAddBottomSheet(
            preselectedMealType = preselectedMealType,
            onDismiss = viewModel::dismissQuickAddSheet,
            onAdd = viewModel::addQuickEntry
        )
    }

    if (state.showCreateCustomFoodSheet) {
        CreateCustomFoodBottomSheet(
            isSubmitting = state.isSubmittingCustomFood,
            onDismiss = viewModel::dismissCreateCustomFoodSheet,
            onSave = viewModel::createCustomFood
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BarcodeResultsBottomSheet(
    results: List<FoodItem>,
    maxPreviewItems: Int,
    onDismiss: () -> Unit,
    onResultClick: (FoodItem) -> Unit,
    onShowAllResults: () -> Unit
) {
    val maxSheetHeight = LocalConfiguration.current.screenHeightDp.dp * 0.88f
    ModalBottomSheet(
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxSheetHeight)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Scanner-Ergebnisse",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Tippe ein Ergebnis zum Hinzufügen.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
            )

            val previewItems = results.take(maxPreviewItems)
            previewItems.forEachIndexed { index, food ->
                FoodSearchItem(
                    food = food,
                    onClick = { onResultClick(food) }
                )
                if (index < previewItems.lastIndex) {
                    HorizontalDivider()
                }
            }

            if (results.size > maxPreviewItems) {
                HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
                TextButton(
                    onClick = onShowAllResults,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 6.dp)
                ) {
                    Text("Alle Ergebnisse anzeigen")
                }
            }
        }
    }
}

@Composable
private fun FoodSearchItem(
    food: FoodItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = food.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = buildFoodSubtitle(food),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${food.caloriesPer100g.toInt()}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "kcal/100g",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun QuickActionsCard(
    onQuickAddClick: () -> Unit,
    onCreateFoodClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(
            text = "Schnell eintragen",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Kalorien direkt loggen oder ein eigenes Produkt anlegen.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onQuickAddClick,
                modifier = Modifier.weight(1f)
            ) {
                Text("Quick Add")
            }
            OutlinedButton(
                onClick = onCreateFoodClick,
                modifier = Modifier.weight(1f)
            ) {
                Text("Eigenes Produkt")
            }
        }
    }
}

private fun buildFoodSubtitle(food: FoodItem): String {
    val macroLine = "E ${food.proteinPer100g.toInt()}g · F ${food.fatPer100g.toInt()}g · K ${food.carbsPer100g.toInt()}g"
    val sizeParts = buildList {
        food.servingSize?.takeIf { it.isNotBlank() }?.let { add("Portion $it") }
        food.packageSize?.takeIf { it.isNotBlank() }?.let { add("Packung $it") }
    }
    return if (sizeParts.isEmpty()) macroLine else "$macroLine · ${sizeParts.joinToString(" · ")}"
}
