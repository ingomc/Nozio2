package de.ingomc.nozio.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.ingomc.nozio.data.repository.SupplementTimelineItem
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun SupplementsTimelineCard(
    selectedDate: LocalDate,
    items: List<SupplementTimelineItem>,
    onEditClick: () -> Unit,
    onToggleTaken: (supplementId: Long, taken: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm", Locale.GERMAN) }

    LaunchedEffect(selectedDate, items.map { it.id }) {
        if (items.isEmpty()) return@LaunchedEffect
        val targetIndex = resolveSupplementTimelineInitialIndex(
            selectedDate = selectedDate,
            items = items
        )
        listState.scrollToItem(targetIndex)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Supplements",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(onClick = onEditClick) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Supplements bearbeiten"
                )
            }
        }

        if (items.isEmpty()) {
            Text(
                text = "Noch keine Supplements geplant. Tippe auf Bearbeiten, um welche hinzuzufügen.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyRow(
                state = listState,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    SupplementTimelineNode(
                        item = item,
                        timeFormatter = timeFormatter,
                        onClick = { onToggleTaken(item.id, !item.isTaken) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SupplementTimelineNode(
    item: SupplementTimelineItem,
    timeFormatter: DateTimeFormatter,
    onClick: () -> Unit
) {
    val toggleBackgroundColor = animateColorAsState(
        targetValue = if (item.isTaken) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
        animationSpec = tween(durationMillis = 180),
        label = "supplementToggleBg"
    )
    val toggleBorderColor = animateColorAsState(
        targetValue = if (item.isTaken) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        animationSpec = tween(durationMillis = 180),
        label = "supplementToggleBorder"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            modifier = Modifier
                .width(176.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${
                            LocalTime.of(
                                item.scheduledMinutesOfDay / 60,
                                item.scheduledMinutesOfDay % 60
                            ).format(timeFormatter)
                        } · ${item.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(toggleBackgroundColor.value)
                            .border(
                                width = 1.dp,
                                color = toggleBorderColor.value,
                                shape = CircleShape
                            )
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = item.isTaken,
                            enter = fadeIn(animationSpec = tween(140)) + scaleIn(animationSpec = tween(180)),
                            exit = fadeOut(animationSpec = tween(100)) + scaleOut(animationSpec = tween(100))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }

                Text(
                    text = "${formatAmount(item.amountValue)} ${item.amountUnit.displayName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun formatAmount(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        String.format(Locale.GERMAN, "%.1f", value)
    }
}
