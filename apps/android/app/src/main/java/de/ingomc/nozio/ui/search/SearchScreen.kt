package de.ingomc.nozio.ui.search

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import de.ingomc.nozio.data.local.FoodItem
import de.ingomc.nozio.data.local.MealType
import de.ingomc.nozio.ui.theme.nozioColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    var showBarcodeScannerSheet by remember { mutableStateOf(false) }
    var pendingCameraAction by remember { mutableStateOf<CameraAction?>(null) }
    var pendingUploadTarget by remember { mutableStateOf<NutritionApplyTarget?>(null) }
    var barcodeScanTarget by remember { mutableStateOf(BarcodeScanTarget.SEARCH) }
    var customFoodScannedBarcode by remember { mutableStateOf<String?>(null) }
    var errorDetailsDialog by remember { mutableStateOf<String?>(null) }
    var searchContainerHeightPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    val listBottomPadding = with(density) { searchContainerHeightPx.toDp() + 28.dp }
    val showingSuggestions = state.query.length < 3
    val hasAnySuggestions = state.recentSuggestions.isNotEmpty() ||
        state.frequentSuggestions.isNotEmpty() ||
        state.favoriteSuggestions.isNotEmpty()
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            when (pendingCameraAction) {
                CameraAction.BARCODE_SEARCH -> {
                    barcodeScanTarget = BarcodeScanTarget.SEARCH
                    showBarcodeScannerSheet = true
                }
                CameraAction.BARCODE_CUSTOM -> {
                    barcodeScanTarget = BarcodeScanTarget.CUSTOM_FOOD
                    showBarcodeScannerSheet = true
                }
                CameraAction.NUTRITION_QUICK -> viewModel.showNutritionScannerSheet(NutritionApplyTarget.QUICK_ADD)
                CameraAction.NUTRITION_CUSTOM -> viewModel.showNutritionScannerSheet(NutritionApplyTarget.CUSTOM_FOOD)
                null -> Unit
            }
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Kamera-Berechtigung benötigt")
            }
        }
        pendingCameraAction = null
    }
    val nutritionImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        val target = pendingUploadTarget
        pendingUploadTarget = null
        if (uri == null || target == null) return@rememberLauncherForActivityResult
        coroutineScope.launch {
            val imageBase64 = withContext(Dispatchers.IO) {
                prepareNutritionImageFromUri(context, uri)
            }
            if (imageBase64.isNullOrBlank()) {
                snackbarHostState.showSnackbar("Bild konnte nicht gelesen werden.")
                return@launch
            }
            viewModel.onNutritionImageCaptured(
                imageBase64 = imageBase64,
                showScannerSheet = false
            )
        }
    }
    var pendingFoodPhotoUpload by remember { mutableStateOf(false) }
    val foodPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        pendingFoodPhotoUpload = false
        if (uri == null) return@rememberLauncherForActivityResult
        coroutineScope.launch {
            val imageBase64 = withContext(Dispatchers.IO) {
                prepareNutritionImageFromUri(context, uri)
            }
            if (imageBase64.isNullOrBlank()) {
                snackbarHostState.showSnackbar("Bild konnte nicht gelesen werden.")
                return@launch
            }
            viewModel.onFoodPhotoReady(imageBase64)
        }
    }
    val launchCameraFlow: (CameraAction) -> Unit = { action ->
        pendingCameraAction = action
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            when (action) {
                CameraAction.BARCODE_SEARCH -> {
                    barcodeScanTarget = BarcodeScanTarget.SEARCH
                    showBarcodeScannerSheet = true
                }
                CameraAction.BARCODE_CUSTOM -> {
                    barcodeScanTarget = BarcodeScanTarget.CUSTOM_FOOD
                    showBarcodeScannerSheet = true
                }
                CameraAction.NUTRITION_QUICK -> viewModel.showNutritionScannerSheet(NutritionApplyTarget.QUICK_ADD)
                CameraAction.NUTRITION_CUSTOM -> viewModel.showNutritionScannerSheet(NutritionApplyTarget.CUSTOM_FOOD)
            }
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    val launchBarcodeScanner: () -> Unit = { launchCameraFlow(CameraAction.BARCODE_SEARCH) }
    val launchCustomFoodBarcodeScanner: () -> Unit = { launchCameraFlow(CameraAction.BARCODE_CUSTOM) }
    val launchQuickAddNutritionScanner: () -> Unit = { launchCameraFlow(CameraAction.NUTRITION_QUICK) }
    val launchCustomFoodNutritionScanner: () -> Unit = { launchCameraFlow(CameraAction.NUTRITION_CUSTOM) }
    val launchQuickAddNutritionUpload: () -> Unit = {
        pendingUploadTarget = NutritionApplyTarget.QUICK_ADD
        viewModel.prepareNutritionImageUpload(NutritionApplyTarget.QUICK_ADD)
        nutritionImagePickerLauncher.launch("image/*")
    }
    val launchCustomFoodNutritionUpload: () -> Unit = {
        pendingUploadTarget = NutritionApplyTarget.CUSTOM_FOOD
        viewModel.prepareNutritionImageUpload(NutritionApplyTarget.CUSTOM_FOOD)
        nutritionImagePickerLauncher.launch("image/*")
    }
    val launchFoodPhotoUpload: () -> Unit = {
        pendingFoodPhotoUpload = true
        foodPhotoPickerLauncher.launch("image/*")
    }

    BackHandler(
        enabled = showBarcodeScannerSheet ||
            state.showNutritionScannerSheet ||
            state.showNutritionReviewSheet ||
            state.showBarcodeResultsSheet ||
            state.showBottomSheet ||
            state.showQuickAddSheet ||
            state.showCreateCustomFoodSheet ||
            state.showFoodPhotoSheet
    ) {
        when {
            showBarcodeScannerSheet -> showBarcodeScannerSheet = false
            state.showFoodPhotoSheet && !state.isFoodPhotoAnalyzing -> viewModel.dismissFoodPhotoSheet()
            state.showNutritionScannerSheet && !state.isNutritionScanInFlight -> viewModel.dismissNutritionScannerSheet()
            state.showNutritionReviewSheet -> viewModel.dismissNutritionReviewSheet()
            state.showBarcodeResultsSheet -> viewModel.dismissBarcodeResultsSheet()
            state.showBottomSheet -> viewModel.dismissBottomSheet()
            state.showQuickAddSheet -> viewModel.dismissQuickAddSheet()
            state.showCreateCustomFoodSheet -> viewModel.dismissCreateCustomFoodSheet()
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

    LaunchedEffect(state.error) {
        val error = state.error
        if (error != null) {
            val result = snackbarHostState.showSnackbar(
                message = error,
                actionLabel = "Details",
                withDismissAction = true,
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) {
                errorDetailsDialog = error
            }
            viewModel.clearError()
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
                if (state.query.length < 3 && !hasAnySuggestions) {
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

                if (showingSuggestions) {
                    SuggestionsSection(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        recentSuggestions = state.recentSuggestions,
                        frequentSuggestions = state.frequentSuggestions,
                        favoriteSuggestions = state.favoriteSuggestions,
                        hasAnySuggestions = hasAnySuggestions,
                        listBottomPadding = listBottomPadding,
                        onQuickAddClick = viewModel::showQuickAddSheet,
                        onCreateFoodClick = viewModel::showCreateCustomFoodSheet,
                        onFoodPhotoClick = launchFoodPhotoUpload,
                        onFoodClick = viewModel::selectFood
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(bottom = listBottomPadding)
                    ) {
                        items(state.results) { food ->
                            FoodSearchItem(
                                food = food,
                                onClick = { viewModel.selectFood(food) }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .imePadding()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onSizeChanged { searchContainerHeightPx = it.height },
                    color = MaterialTheme.nozioColors.surface2,
                    shape = CircleShape,
                    tonalElevation = 2.dp,
                    shadowElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
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
                                focusedContainerColor = MaterialTheme.nozioColors.baseBgElevated,
                                unfocusedContainerColor = MaterialTheme.nozioColors.baseBgElevated,
                                disabledContainerColor = MaterialTheme.nozioColors.baseBgElevated,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
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

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 72.dp)
            )
        }
    }

    if (showBarcodeScannerSheet) {
        BarcodeScannerBottomSheet(
            onDismiss = { showBarcodeScannerSheet = false },
            onBarcodeDetected = { barcode ->
                showBarcodeScannerSheet = false
                val normalizedBarcode = barcode.filter(Char::isDigit)
                when (barcodeScanTarget) {
                    BarcodeScanTarget.SEARCH -> viewModel.onBarcodeScanned(normalizedBarcode)
                    BarcodeScanTarget.CUSTOM_FOOD -> customFoodScannedBarcode = normalizedBarcode
                }
            }
        )
    }

    if (state.showNutritionScannerSheet) {
        NutritionScannerBottomSheet(
            isAnalyzing = state.isNutritionScanInFlight,
            onDismiss = {
                if (!state.isNutritionScanInFlight) {
                    viewModel.dismissNutritionScannerSheet()
                }
            },
            onImageBase64Captured = { imageBase64 ->
                viewModel.onNutritionImageCaptured(imageBase64)
            }
        )
    }

    if (state.showFoodPhotoSheet) {
        FoodPhotoAnalysisBottomSheet(
            isAnalyzing = state.isFoodPhotoAnalyzing,
            onDismiss = {
                if (!state.isFoodPhotoAnalyzing) {
                    viewModel.dismissFoodPhotoSheet()
                }
            },
            onSubmit = { portionSize, hints ->
                viewModel.submitFoodPhotoAnalysis(portionSize = portionSize, hints = hints)
            }
        )
    }

    if (state.showNutritionReviewSheet && state.nutritionScanResult != null) {
        NutritionReviewBottomSheet(
            scanResult = state.nutritionScanResult!!,
            applyButtonLabel = if (state.nutritionApplyTarget == NutritionApplyTarget.QUICK_ADD) {
                "In Quick Add uebernehmen"
            } else {
                "In Eigenes Produkt uebernehmen"
            },
            onDismiss = viewModel::dismissNutritionReviewSheet,
            onApply = viewModel::applyNutritionDraft
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
            },
            onToggleFavorite = viewModel::toggleSelectedFoodFavorite
        )
    }

    if (state.showQuickAddSheet) {
        QuickAddBottomSheet(
            preselectedMealType = preselectedMealType,
            initial = state.prefillQuickAddDraft,
            onScanNutrition = launchQuickAddNutritionScanner,
            onUploadNutritionImage = launchQuickAddNutritionUpload,
            onDismiss = viewModel::dismissQuickAddSheet,
            onAdd = viewModel::addQuickEntry
        )
    }

    if (state.showCreateCustomFoodSheet) {
        CreateCustomFoodBottomSheet(
            isSubmitting = state.isSubmittingCustomFood,
            initial = state.prefillCustomFoodDraft,
            prefilledBarcode = customFoodScannedBarcode,
            onScanNutrition = launchCustomFoodNutritionScanner,
            onUploadNutritionImage = launchCustomFoodNutritionUpload,
            onScanBarcode = launchCustomFoodBarcodeScanner,
            onDismiss = viewModel::dismissCreateCustomFoodSheet,
            onSave = viewModel::createCustomFood
        )
    }

    errorDetailsDialog?.let { message ->
        AlertDialog(
            onDismissRequest = { errorDetailsDialog = null },
            confirmButton = {
                TextButton(onClick = { errorDetailsDialog = null }) {
                    Text("OK")
                }
            },
            title = { Text("Fehlerdetails") },
            text = { Text(message) }
        )
    }
}

private enum class CameraAction {
    BARCODE_SEARCH,
    BARCODE_CUSTOM,
    NUTRITION_QUICK,
    NUTRITION_CUSTOM
}

private enum class BarcodeScanTarget {
    SEARCH,
    CUSTOM_FOOD
}

private enum class SuggestionTab(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    RECENT("Zuletzt", Icons.Default.History),
    FREQUENT("Häufig", Icons.Default.Repeat),
    FAVORITES("Favoriten", Icons.Default.Favorite)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SuggestionsSection(
    modifier: Modifier = Modifier,
    recentSuggestions: List<FoodItem>,
    frequentSuggestions: List<FoodItem>,
    favoriteSuggestions: List<FoodItem>,
    hasAnySuggestions: Boolean,
    listBottomPadding: androidx.compose.ui.unit.Dp,
    onQuickAddClick: () -> Unit,
    onCreateFoodClick: () -> Unit,
    onFoodPhotoClick: () -> Unit,
    onFoodClick: (FoodItem) -> Unit
) {
    val tabs = remember { SuggestionTab.entries }
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier
    ) {
        QuickActionsCard(
            onQuickAddClick = onQuickAddClick,
            onCreateFoodClick = onCreateFoodClick,
            onFoodPhotoClick = onFoodPhotoClick
        )
        HorizontalDivider()

        if (!hasAnySuggestions) {
            return@Column
        }

        PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = null
                        )
                    },
                    text = {
                        Text(
                            text = tab.label,
                            maxLines = 1
                        )
                    }
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { page ->
            val foods = when (tabs[page]) {
                SuggestionTab.RECENT -> recentSuggestions
                SuggestionTab.FREQUENT -> frequentSuggestions
                SuggestionTab.FAVORITES -> favoriteSuggestions
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = listBottomPadding)
            ) {
                if (foods.isEmpty()) {
                    item {
                        Text(
                            text = when (tabs[page]) {
                                SuggestionTab.RECENT -> "Noch keine zuletzt hinzugefügten Produkte."
                                SuggestionTab.FREQUENT -> "Noch keine häufig hinzugefügten Produkte."
                                SuggestionTab.FAVORITES -> "Noch keine Favoriten."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                        )
                    }
                } else {
                    items(foods) { food ->
                        FoodSearchItem(
                            food = food,
                            onClick = { onFoodClick(food) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
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
        containerColor = MaterialTheme.nozioColors.surface2,
        shape = MaterialTheme.shapes.extraLarge
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
        verticalAlignment = Alignment.Top
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
    onCreateFoodClick: () -> Unit,
    onFoodPhotoClick: () -> Unit
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
            text = "Kalorien loggen oder ein eigenes Produkt anlegen.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onQuickAddClick,
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Quick Add",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelLarge
                )
            }
            OutlinedButton(
                onClick = onCreateFoodClick,
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Produkt",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
        OutlinedButton(
            onClick = onFoodPhotoClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .height(40.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Essen per Foto erkennen",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelLarge
            )
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
