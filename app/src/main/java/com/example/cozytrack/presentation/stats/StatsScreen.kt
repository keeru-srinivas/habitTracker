package com.example.cozytrack.presentation.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cozytrack.domain.model.Frequency
import com.example.cozytrack.domain.model.Habit
import com.example.cozytrack.presentation.components.BrownPrimary
import com.example.cozytrack.presentation.components.DeepText
import com.example.cozytrack.presentation.components.GreenDone
import com.example.cozytrack.presentation.components.MutedText
import com.example.cozytrack.presentation.components.OrangeDone
import com.example.cozytrack.presentation.components.PurplePrimary
import com.example.cozytrack.presentation.components.ScreenBackground
import com.example.cozytrack.presentation.components.shortDateLabel
import com.example.cozytrack.presentation.habits.HabitHeatmapDay
import com.example.cozytrack.presentation.habits.HabitListViewModel

data class ConsistencyPoint(
    val date: String,
    val percentage: Float
)

@Composable
fun StatsScreen(
    viewModel: HabitListViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val activeHabits = state.habits.filter { !it.isArchived }
    var selectedHabitId by rememberSaveable { mutableStateOf<String?>(null) }

    val chartHabits = if (selectedHabitId == null) {
        activeHabits
    } else {
        activeHabits.filter { it.id == selectedHabitId }
    }

    val consistencyPoints = computeConsistencyTrend(
        habits = chartHabits,
        heatmapByHabitId = state.heatmapByHabitId,
        days = 30
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Consistency stats",
                color = DeepText,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "Track how consistently you complete habits over the last 30 days",
                color = MutedText,
                style = MaterialTheme.typography.bodySmall
            )
        }

        if (activeHabits.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedHabitId == null,
                        onClick = { selectedHabitId = null },
                        label = { Text("All habits") },
                        colors = statsChipColors()
                    )
                    activeHabits.take(4).forEach { habit ->
                        FilterChip(
                            selected = selectedHabitId == habit.id,
                            onClick = { selectedHabitId = habit.id },
                            label = { Text(habit.title.take(12)) },
                            colors = statsChipColors()
                        )
                    }
                }
            }
        }

        item {
            SummaryCards(
                habits = chartHabits,
                progressByHabitId = state.progressByHabitId,
                consistencyPoints = consistencyPoints
            )
        }

        item {
            ConsistencyChartCard(
                points = consistencyPoints,
                isLoading = state.isLoading && consistencyPoints.isEmpty()
            )
        }

        if (chartHabits.isNotEmpty()) {
            item {
                Text(
                    text = "Streak overview",
                    color = DeepText,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            items(chartHabits) { habit ->
                StreakOverviewCard(
                    habit = habit,
                    progress = state.progressByHabitId[habit.id],
                    heatmapDays = state.heatmapByHabitId[habit.id].orEmpty()
                )
            }
        }
    }
}

@Composable
private fun SummaryCards(
    habits: List<Habit>,
    progressByHabitId: Map<String, com.example.cozytrack.domain.model.HabitProgress>,
    consistencyPoints: List<ConsistencyPoint>
) {
    val avgCompletion = if (habits.isEmpty()) {
        0
    } else {
        habits.mapNotNull { progressByHabitId[it.id]?.completionRate }
            .average()
            .let { if (it.isNaN()) 0.0 else it * 100 }
            .toInt()
    }
    val recentConsistency = consistencyPoints.lastOrNull()?.percentage?.toInt() ?: 0
    val bestStreak = habits.maxOfOrNull { progressByHabitId[it.id]?.bestStreak ?: 0 } ?: 0

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatCard(
            label = "Recent",
            value = "$recentConsistency%",
            tint = GreenDone,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Avg rate",
            value = "$avgCompletion%",
            tint = OrangeDone,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Best streak",
            value = "$bestStreak",
            tint = BrownPrimary,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                color = MutedText,
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = value,
                color = tint,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}

@Composable
private fun ConsistencyChartCard(
    points: List<ConsistencyPoint>,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "30-day consistency",
                color = DeepText,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Higher line = more habits completed each day",
                color = MutedText,
                style = MaterialTheme.typography.bodySmall
            )

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = BrownPrimary)
                }
            } else if (points.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Add habits on Home to see your consistency trend",
                        color = MutedText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                ConsistencyLineChart(
                    points = points,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = points.first().date.shortDateLabel(),
                        color = MutedText,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = points.last().date.shortDateLabel(),
                        color = MutedText,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
private fun ConsistencyLineChart(
    points: List<ConsistencyPoint>,
    modifier: Modifier = Modifier
) {
    val lineColor = BrownPrimary
    val fillColor = BrownPrimary.copy(alpha = 0.12f)
    val gridColor = MutedText.copy(alpha = 0.2f)

    Canvas(modifier = modifier) {
        if (points.size < 2) return@Canvas

        val chartLeft = 8f
        val chartRight = size.width - 8f
        val chartTop = 12f
        val chartBottom = size.height - 24f
        val chartWidth = chartRight - chartLeft
        val chartHeight = chartBottom - chartTop

        for (level in listOf(0f, 25f, 50f, 75f, 100f)) {
            val y = chartBottom - (level / 100f) * chartHeight
            drawLine(
                color = gridColor,
                start = Offset(chartLeft, y),
                end = Offset(chartRight, y),
                strokeWidth = 1f
            )
        }

        val path = Path()
        val fillPath = Path()
        points.forEachIndexed { index, point ->
            val x = chartLeft + (index.toFloat() / (points.lastIndex.coerceAtLeast(1))) * chartWidth
            val y = chartBottom - (point.percentage / 100f) * chartHeight
            if (index == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, chartBottom)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }

        val lastX = chartLeft + chartWidth
        fillPath.lineTo(lastX, chartBottom)
        fillPath.close()
        drawPath(path = fillPath, color = fillColor)
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 4f, cap = StrokeCap.Round)
        )

        points.forEachIndexed { index, point ->
            val x = chartLeft + (index.toFloat() / (points.lastIndex.coerceAtLeast(1))) * chartWidth
            val y = chartBottom - (point.percentage / 100f) * chartHeight
            drawCircle(
                color = if (point.percentage >= 75f) GreenDone else lineColor,
                radius = 5f,
                center = Offset(x, y)
            )
        }
    }
}

@Composable
private fun StreakOverviewCard(
    habit: Habit,
    progress: com.example.cozytrack.domain.model.HabitProgress?,
    heatmapDays: List<HabitHeatmapDay>
) {
    val unitLabel = when (habit.frequency) {
        Frequency.Daily -> "days"
        Frequency.Weekly -> "weeks"
    }
    val currentStreak = when (habit.frequency) {
        Frequency.Daily -> heatmapDays.currentCompletedStreak()
        Frequency.Weekly -> progress?.currentStreak ?: 0
    }
    val bestStreak = when (habit.frequency) {
        Frequency.Daily -> heatmapDays.bestCompletedStreak()
        Frequency.Weekly -> progress?.bestStreak ?: 0
    }
    val isConsistent = (progress?.completionRate ?: 0.0) >= 0.7

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = habit.title,
                    color = DeepText,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "Current: $currentStreak $unitLabel · Best: $bestStreak $unitLabel",
                    color = MutedText,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(
                text = if (isConsistent) "Consistent" else "Building",
                color = if (isConsistent) GreenDone else OrangeDone,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun statsChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = PurplePrimary,
    selectedLabelColor = Color.White,
    containerColor = Color.White,
    labelColor = DeepText
)

fun computeConsistencyTrend(
    habits: List<Habit>,
    heatmapByHabitId: Map<String, List<HabitHeatmapDay>>,
    days: Int
): List<ConsistencyPoint> {
    if (habits.isEmpty()) return emptyList()

    val referenceDates = heatmapByHabitId.values
        .firstOrNull { it.isNotEmpty() }
        ?.takeLast(days)
        ?.map { it.calendarDate }
        .orEmpty()

    if (referenceDates.isEmpty()) return emptyList()

    return referenceDates.map { date ->
        val completedCount = habits.count { habit ->
            heatmapByHabitId[habit.id]
                ?.firstOrNull { it.calendarDate == date }
                ?.completed == true
        }
        val percentage = (completedCount.toFloat() / habits.size.toFloat()) * 100f
        ConsistencyPoint(date = date, percentage = percentage)
    }
}

private fun List<HabitHeatmapDay>.currentCompletedStreak(): Int {
    val todayIndex = indexOfLast { it.isToday }
    if (todayIndex == -1 || !this[todayIndex].completed) return 0

    var streak = 0
    for (index in todayIndex downTo 0) {
        if (!this[index].completed) break
        streak += 1
    }
    return streak
}

private fun List<HabitHeatmapDay>.bestCompletedStreak(): Int {
    var best = 0
    var current = 0

    forEach { day ->
        if (day.completed) {
            current += 1
            best = maxOf(best, current)
        } else {
            current = 0
        }
    }

    return best
}
