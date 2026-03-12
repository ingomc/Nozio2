package de.ingomc.nozio.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import de.ingomc.nozio.ui.common.bringIntoViewOnFocus
import de.ingomc.nozio.ui.theme.nozioColors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FoodPhotoAnalysisBottomSheet(
    isAnalyzing: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (portionSize: String, hints: List<String>) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val maxSheetHeight = LocalConfiguration.current.screenHeightDp.dp * 0.88f

    var selectedPortion by rememberSaveable { mutableStateOf("medium") }
    var selectedProtein by rememberSaveable { mutableStateOf<String?>(null) }
    var additionalNotes by rememberSaveable { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = {
            if (!isAnalyzing) onDismiss()
        },
        sheetState = sheetState,
        containerColor = MaterialTheme.nozioColors.surface2,
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxSheetHeight)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Essen analysieren",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Beantworte kurz ein paar Fragen fuer genauere Ergebnisse.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            )

            Text(
                text = "Wie gross ist die Portion?",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            ) {
                PortionOption.entries.forEach { option ->
                    FilterChip(
                        selected = selectedPortion == option.value,
                        onClick = { selectedPortion = option.value },
                        label = { Text(option.label) }
                    )
                }
            }

            Text(
                text = "Welches Protein ist enthalten?",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            ) {
                ProteinOption.entries.forEach { option ->
                    FilterChip(
                        selected = selectedProtein == option.value,
                        onClick = {
                            selectedProtein = if (selectedProtein == option.value) null else option.value
                        },
                        label = { Text(option.label) }
                    )
                }
            }

            Text(
                text = "Weitere Hinweise (optional)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value = additionalNotes,
                onValueChange = { additionalNotes = it },
                placeholder = { Text("z.B. mit Sosse, Reis als Beilage...") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewOnFocus()
                    .padding(bottom = 24.dp)
            )

            Button(
                onClick = {
                    val hints = buildList {
                        selectedProtein?.let { add(it) }
                        additionalNotes.trim().takeIf { it.isNotBlank() }?.let { add(it) }
                    }
                    onSubmit(selectedPortion, hints)
                },
                enabled = !isAnalyzing,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isAnalyzing) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 8.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Text("Analyse laeuft...")
                } else {
                    Text("Essen analysieren")
                }
            }

            if (isAnalyzing) {
                Text(
                    text = "Die KI analysiert dein Foto. Das kann einige Sekunden dauern.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

private enum class PortionOption(val value: String, val label: String) {
    SMALL("small", "Klein"),
    MEDIUM("medium", "Mittel"),
    LARGE("large", "Gross")
}

private enum class ProteinOption(val value: String, val label: String) {
    CHICKEN("Haehnchen", "Haehnchen"),
    BEEF("Rind", "Rind"),
    PORK("Schwein", "Schwein"),
    FISH("Fisch", "Fisch"),
    VEGETARIAN("Vegetarisch", "Vegetarisch"),
    VEGAN("Vegan", "Vegan")
}
