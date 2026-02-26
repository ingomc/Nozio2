package de.ingomc.nozio.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun ActivityCard(
    steps: Long,
    activeCalories: Double,
    stepsInput: String,
    stepsSaved: Boolean,
    onStepsInputChange: (String) -> Unit,
    onSaveSteps: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ActivityItem(icon = Icons.Default.DirectionsRun, label = "Schritte", value = steps.toString())
                ActivityItem(
                    icon = Icons.Default.LocalFireDepartment,
                    label = "Aktivitätskalorien",
                    value = "%.0f".format(activeCalories)
                )
            }

            OutlinedTextField(
                value = stepsInput,
                onValueChange = onStepsInputChange,
                label = { Text("Schritte heute") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            FilledTonalButton(
                onClick = onSaveSteps,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (stepsSaved) "Schritte gespeichert ✓" else "Schritte speichern")
            }
        }
    }
}

@Composable
private fun ActivityItem(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Icon(imageVector = icon, contentDescription = label)
        Text(text = value, style = MaterialTheme.typography.headlineSmall)
        Text(text = label, style = MaterialTheme.typography.bodySmall)
    }
}
