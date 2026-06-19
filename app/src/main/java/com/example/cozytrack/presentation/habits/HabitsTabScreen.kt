package com.example.cozytrack.presentation.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cozytrack.domain.model.Frequency
import com.example.cozytrack.domain.model.Habit
import com.example.cozytrack.domain.model.HabitProgress
import com.example.cozytrack.presentation.components.BrownPrimary
import com.example.cozytrack.presentation.components.DeepText
import com.example.cozytrack.presentation.components.GreenDone
import com.example.cozytrack.presentation.components.MutedText
import com.example.cozytrack.presentation.components.OrangeDone
import com.example.cozytrack.presentation.components.ScreenBackground

@Composable
fun HabitsTabScreen(
    viewModel: HabitListViewModel,
    onHabitClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val filteredHabits = state.filteredHabits

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Your habits",
                color = DeepText,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "Tap a habit to view streaks and history",
                color = MutedText,
                style = MaterialTheme.typography.bodySmall
            )
        }

        item {
            HabitFrequencyFilterChips(
                selected = state.habitListFrequencyFilter,
                onSelectedChange = viewModel::onHabitListFrequencyFilterChange
            )
        }

        if (state.isLoading && state.habits.isEmpty()) {
            item {
                CircularProgressIndicator(color = BrownPrimary)
            }
        }

        if (state.habits.isEmpty() && !state.isLoading) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Text(
                        text = "No habits yet. Create one from the Home tab.",
                        modifier = Modifier.padding(18.dp),
                        color = MutedText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        if (filteredHabits.isEmpty() && !state.isLoading && state.habits.isNotEmpty()) {
            item {
                val label = when (state.habitListFrequencyFilter) {
                    Frequency.Daily -> "daily"
                    Frequency.Weekly -> "weekly"
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Text(
                        text = "No $label habits. Switch to the other filter or create one on Home.",
                        modifier = Modifier.padding(18.dp),
                        color = MutedText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        items(filteredHabits) { habit ->
            SimpleHabitListItem(
                habit = habit,
                progress = state.progressByHabitId[habit.id],
                checkedToday = habit.id in state.checkedHabitIds,
                onClick = { onHabitClick(habit.id) }
            )
        }
    }
}

@Composable
private fun SimpleHabitListItem(
    habit: Habit,
    progress: HabitProgress?,
    checkedToday: Boolean,
    onClick: () -> Unit
) {
    val unitLabel = when (habit.frequency) {
        Frequency.Daily -> "days"
        Frequency.Weekly -> "weeks"
    }
    val currentStreak = progress?.currentStreak ?: 0
    val completionRate = progress?.completionRate?.let { (it * 100).toInt() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFF4DEC5)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = if (checkedToday) "✓" else "👟", color = BrownPrimary)
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = habit.title,
                    color = DeepText,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = buildString {
                        append(if (habit.frequency == Frequency.Daily) "Daily" else "Weekly")
                        if (habit.isArchived) append(" · Archived")
                        append(" · $currentStreak $unitLabel streak")
                    },
                    color = MutedText,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (checkedToday) "Done" else "Pending",
                    color = if (checkedToday) GreenDone else OrangeDone,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium
                )
                if (completionRate != null) {
                    Text(
                        text = "$completionRate% rate",
                        color = MutedText,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}
