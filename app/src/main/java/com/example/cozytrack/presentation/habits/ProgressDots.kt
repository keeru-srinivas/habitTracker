package com.example.cozytrack.presentation.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.cozytrack.domain.model.ProgressSnapshot

@Composable
fun ProgressDots(
    snapshots: List<ProgressSnapshot>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        snapshots.forEach { snapshot ->
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(colorForStatus(snapshot.status))
            )
        }
    }
}

@Composable
private fun colorForStatus(status: String): Color {
    return when (status.lowercase()) {
        "done", "completed", "complete" -> MaterialTheme.colorScheme.primary
        "missed" -> MaterialTheme.colorScheme.error
        "skipped" -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
}
