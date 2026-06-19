package com.example.cozytrack.presentation.habits

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cozytrack.domain.model.Frequency
import com.example.cozytrack.domain.model.Habit
import com.example.cozytrack.domain.model.HabitProgress
import com.example.cozytrack.presentation.components.BrownPrimary
import com.example.cozytrack.presentation.components.CreamCard
import com.example.cozytrack.presentation.components.DeepText
import com.example.cozytrack.presentation.components.DeleteRed
import com.example.cozytrack.presentation.components.GreenDone
import com.example.cozytrack.presentation.components.MutedText
import com.example.cozytrack.presentation.components.OrangeDone
import com.example.cozytrack.presentation.components.PinkToday
import com.example.cozytrack.presentation.components.PurplePrimary
import com.example.cozytrack.presentation.components.ScreenBackground
import com.example.cozytrack.presentation.components.prettyDate

@Composable
fun HabitListScreen(
    viewModel: HabitListViewModel,
    onHabitClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val errorMessage = state.errorMessage
    val filteredHabits = state.filteredHabits

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            HeaderSection(
                utcCalendarDate = state.utcCalendarDate,
                userName = state.userName,
                thoughtText = state.thought?.let { thought ->
                    if (thought.author.isNullOrBlank()) {
                        thought.quote
                    } else {
                        "${thought.quote} - ${thought.author}"
                    }
                }
            )
        }

            item {
                CreateHabitSection(
                    title = state.newHabitTitle,
                    frequency = state.newHabitFrequency,
                    onTitleChange = viewModel::onNewHabitTitleChange,
                    onFrequencyChange = viewModel::onFrequencyChange,
                    onCreateClick = viewModel::createHabit
                )
            }

            item {
                ArchiveToggleCard(
                    checked = state.includeArchived,
                    onCheckedChange = viewModel::onIncludeArchivedChange
                )
            }

            item {
                HabitFrequencyFilterChips(
                    selected = state.habitListFrequencyFilter,
                    onSelectedChange = viewModel::onHabitListFrequencyFilterChange
                )
            }

            if (errorMessage != null) {
                item {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            if (state.isLoading) {
                item {
                    CircularProgressIndicator()
                }
            }

            if (filteredHabits.isEmpty() && !state.isLoading && state.habits.isNotEmpty()) {
                item {
                    EmptyFilteredHabitsCard(frequency = state.habitListFrequencyFilter)
                }
            }

            items(filteredHabits) { habit ->
                HabitCard(
                    habit = habit,
                    progress = state.progressByHabitId[habit.id],
                    heatmapDays = state.heatmapByHabitId[habit.id].orEmpty(),
                    checkInDate = state.utcCalendarDate,
                    checked = habit.id in state.checkedHabitIds,
                    isEditing = state.editingHabitId == habit.id,
                    editingTitle = state.editingHabitTitle,
                    editingFrequency = state.editingHabitFrequency,
                    onCheckedChange = { checked ->
                        viewModel.onHabitCheckedChange(habit.id, checked)
                    },
                    onEditClick = { viewModel.startEditingHabit(habit) },
                    onArchiveClick = { viewModel.archiveHabit(habit) },
                    onRestoreClick = { viewModel.restoreHabit(habit) },
                    onDeleteClick = { viewModel.deleteHabit(habit.id) },
                    onEditTitleChange = viewModel::onEditingHabitTitleChange,
                    onEditFrequencyChange = viewModel::onEditingHabitFrequencyChange,
                    onSaveEditClick = viewModel::saveHabitEdit,
                    onCancelEditClick = viewModel::cancelEditingHabit,
                    onHabitClick = { onHabitClick(habit.id) }
                )
            }
        }
}

@Composable
fun HabitDetailScreen(
    viewModel: HabitListViewModel,
    habitId: String,
    onBackClick: () -> Unit,
    onLoggedOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val habit = state.habits.firstOrNull { it.id == habitId }
    val errorMessage = state.errorMessage

    BackHandler(onBack = onBackClick)

    LaunchedEffect(state.isLoggedOut) {
        if (state.isLoggedOut) {
            onLoggedOut()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onBackClick,
                        colors = ButtonDefaults.textButtonColors(contentColor = BrownPrimary)
                    ) {
                        Text("← Back")
                    }
                    Text(
                        text = state.utcCalendarDate.prettyDate(),
                        color = MutedText,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            if (errorMessage != null) {
                item {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            if (state.isLoading || habit == null) {
                item {
                    CircularProgressIndicator()
                }
            }

            if (habit != null) {
                item {
                    HeatmapHeader(
                        habit = habit,
                        onEditClick = { viewModel.startEditingHabit(habit) },
                        onArchiveClick = { viewModel.archiveHabit(habit) },
                        onRestoreClick = { viewModel.restoreHabit(habit) },
                        onDeleteClick = {
                            viewModel.deleteHabit(habit.id)
                            onBackClick()
                        }
                    )
                }

                item {
                    TodayCheckInSection(
                        checkInDate = state.utcCalendarDate,
                        checked = habit.id in state.checkedHabitIds,
                        enabled = !habit.isArchived,
                        onCheckedChange = { checked ->
                            viewModel.onHabitCheckedChange(habit.id, checked)
                        }
                    )
                }

                item {
                    ProgressHeatmapCard(
                        progress = state.progressByHabitId[habit.id],
                        frequency = habit.frequency,
                        days = state.heatmapByHabitId[habit.id].orEmpty(),
                        todayDate = state.utcCalendarDate
                    )
                }
            }
    }
}

@Composable
private fun HeaderSection(
    utcCalendarDate: String,
    userName: String,
    thoughtText: String?
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GradientIconBox(text = "🐻")
            Column {
                Text(
                    text = if (userName.isBlank()) {
                        "CozyTrack"
                    } else {
                        "Welcome back, $userName"
                    },
                    color = DeepText,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "Date: ${utcCalendarDate.prettyDate()}",
                    color = MutedText,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        if (!thoughtText.isNullOrBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CreamCard),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "“",
                        color = BrownPrimary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = thoughtText,
                        modifier = Modifier.weight(1f),
                        color = DeepText,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(text = "🐻☕", style = MaterialTheme.typography.headlineSmall)
                }
            }
        }
    }
}

@Composable
fun HabitFrequencyFilterChips(
    selected: Frequency,
    onSelectedChange: (Frequency) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Show habits",
                color = DeepText,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selected == Frequency.Daily,
                    onClick = { onSelectedChange(Frequency.Daily) },
                    label = { Text("☀ Daily") },
                    colors = habitChipColors()
                )

                FilterChip(
                    selected = selected == Frequency.Weekly,
                    onClick = { onSelectedChange(Frequency.Weekly) },
                    label = { Text("📅 Weekly") },
                    colors = habitChipColors()
                )
            }
        }
    }
}

@Composable
private fun EmptyFilteredHabitsCard(frequency: Frequency) {
    val label = when (frequency) {
        Frequency.Daily -> "daily"
        Frequency.Weekly -> "weekly"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Text(
            text = "No $label habits yet. Create one above or switch to the other filter.",
            modifier = Modifier.padding(18.dp),
            color = MutedText,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun CreateHabitSection(
    title: String,
    frequency: Frequency,
    onTitleChange: (String) -> Unit,
    onFrequencyChange: (Frequency) -> Unit,
    onCreateClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SmallPill(text = "🐾")
                Text(
                    text = "Create a new habit",
                    color = DeepText,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                label = { Text("Habit title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PurplePrimary,
                    unfocusedBorderColor = Color(0xFFE4DEF7),
                    focusedLabelColor = PurplePrimary,
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    cursorColor = Color.Black
                )
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = frequency == Frequency.Daily,
                    onClick = { onFrequencyChange(Frequency.Daily) },
                    label = { Text("☀ Daily") },
                    colors = habitChipColors()
                )

                FilterChip(
                    selected = frequency == Frequency.Weekly,
                    onClick = { onFrequencyChange(Frequency.Weekly) },
                    label = { Text("📅 Weekly") },
                    colors = habitChipColors()
                )
            }

            Button(
                onClick = onCreateClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrownPrimary)
            ) {
                Text("⊕  Add habit")
            }
        }
    }
}

@Composable
private fun ArchiveToggleCard(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🗃  Show archived habits",
                color = DeepText,
                style = MaterialTheme.typography.bodyMedium
            )
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Composable
private fun HabitCard(
    habit: Habit,
    progress: HabitProgress?,
    heatmapDays: List<HabitHeatmapDay>,
    checkInDate: String,
    checked: Boolean,
    isEditing: Boolean,
    editingTitle: String,
    editingFrequency: Frequency,
    onCheckedChange: (Boolean) -> Unit,
    onEditClick: () -> Unit,
    onArchiveClick: () -> Unit,
    onRestoreClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onEditTitleChange: (String) -> Unit,
    onEditFrequencyChange: (Frequency) -> Unit,
    onSaveEditClick: () -> Unit,
    onCancelEditClick: () -> Unit,
    onHabitClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (isEditing) {
            EditableHabitHeader(
                title = editingTitle,
                frequency = editingFrequency,
                onTitleChange = onEditTitleChange,
                onFrequencyChange = onEditFrequencyChange,
                onSaveClick = onSaveEditClick,
                onCancelClick = onCancelEditClick
            )
        } else {
            HeatmapHeader(
                habit = habit,
                onEditClick = onEditClick,
                onArchiveClick = onArchiveClick,
                onRestoreClick = onRestoreClick,
                onDeleteClick = onDeleteClick,
                onTitleClick = onHabitClick
            )
        }

        TodayCheckInSection(
            checkInDate = checkInDate,
            checked = checked,
            enabled = !habit.isArchived,
            onCheckedChange = onCheckedChange
        )
        Text(
            text = "Tap the habit title to view its streak and 365-day grid",
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .clickable(onClick = onHabitClick),
            color = MutedText,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun HeatmapHeader(
    habit: Habit,
    onEditClick: () -> Unit,
    onArchiveClick: () -> Unit,
    onRestoreClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onTitleClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .then(
                    if (onTitleClick != null) {
                        Modifier.clickable(onClick = onTitleClick)
                    } else {
                        Modifier
                    }
                ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFF4DEC5)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "👟",
                    color = BrownPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
            Column {
                Text(
                    text = habit.title.uppercase(),
                    color = DeepText,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Last 365 days",
                    color = MutedText,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Row {
                TextButton(
                    onClick = onEditClick,
                    colors = ButtonDefaults.textButtonColors(contentColor = BrownPrimary)
                ) {
                    Text("✎\nEdit")
                }
                TextButton(
                    onClick = if (habit.isArchived) onRestoreClick else onArchiveClick,
                    colors = ButtonDefaults.textButtonColors(contentColor = MutedText)
                ) {
                    Text(if (habit.isArchived) "🧺\nRestore" else "🧺\nArchive")
                }
                TextButton(
                    onClick = onDeleteClick,
                    colors = ButtonDefaults.textButtonColors(contentColor = DeleteRed)
                ) {
                    Text("🗑\nDelete")
                }
            }
        }
    }
}

@Composable
private fun TodayCheckInSection(
    checkInDate: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (checked) Color(0xFFEAF8EF) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "🐻", style = MaterialTheme.typography.headlineMedium)
                Column {
                    Text(
                        text = "Today's status",
                        color = DeepText,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = if (checked) {
                            "Completed for ${checkInDate.prettyDate()}"
                        } else if (enabled) {
                            "Not complete for ${checkInDate.prettyDate()}"
                        } else {
                            "Archived habits cannot be checked in"
                        },
                        color = if (checked) GreenDone else MutedText,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Button(
                enabled = enabled && !checked,
                onClick = { onCheckedChange(true) },
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (checked) GreenDone else PurplePrimary,
                    disabledContainerColor = if (checked) GreenDone else Color(0xFFF7F1E6),
                    disabledContentColor = if (checked) Color.White else GreenDone
                )
            ) {
                Text(if (checked) "Completed" else "Complete today")
            }
        }
    }
}

@Composable
private fun EditableHabitHeader(
    title: String,
    frequency: Frequency,
    onTitleChange: (String) -> Unit,
    onFrequencyChange: (Frequency) -> Unit,
    onSaveClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                label = { Text("Habit title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    cursorColor = Color.Black
                )
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = frequency == Frequency.Daily,
                    onClick = { onFrequencyChange(Frequency.Daily) },
                    label = { Text("☀ Daily") },
                    colors = habitChipColors()
                )

                FilterChip(
                    selected = frequency == Frequency.Weekly,
                    onClick = { onFrequencyChange(Frequency.Weekly) },
                    label = { Text("📅 Weekly") },
                    colors = habitChipColors()
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSaveClick) {
                    Text("Save")
                }
                TextButton(onClick = onCancelClick) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
private fun ProgressHeatmapCard(
    progress: HabitProgress?,
    frequency: Frequency,
    days: List<HabitHeatmapDay>,
    todayDate: String
) {
    val unitLabel = when (frequency) {
        Frequency.Daily -> "days"
        Frequency.Weekly -> "weeks"
    }
    val currentStreak = when (frequency) {
        Frequency.Daily -> days.currentCompletedStreak()
        Frequency.Weekly -> progress?.currentStreak ?: 0
    }
    val bestStreak = when (frequency) {
        Frequency.Daily -> days.bestCompletedStreak()
        Frequency.Weekly -> progress?.bestStreak ?: 0
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StreakStat(
                    label = "Current streak",
                    value = "$currentStreak $unitLabel",
                    modifier = Modifier.weight(1f),
                    tint = OrangeDone,
                    icon = "🔥"
                )
                StreakStat(
                    label = "Best streak",
                    value = "$bestStreak $unitLabel",
                    modifier = Modifier.weight(1f),
                    tint = BrownPrimary,
                    icon = "★"
                )
            }

            HabitHeatmap(days = days)
            Text(
                text = "Tap a square to see that day. ${days.size} day rolling history ending ${todayDate.prettyDate()}.",
                color = MutedText,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun StreakStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    tint: Color,
    icon: String
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFFAF7FF))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(tint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = icon,
                color = tint,
                fontWeight = FontWeight.Bold
            )
        }
        Column {
            Text(
                text = label,
                color = MutedText,
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = value,
                color = DeepText,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium
            )
        }
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

@Composable
private fun GradientIconBox(text: String) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(BrownPrimary, Color(0xFFD1A06F))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
private fun SmallPill(text: String) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(BrownPrimary, Color(0xFFC6864B))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun habitChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = PurplePrimary,
    selectedLabelColor = Color.White,
    containerColor = Color.White,
    labelColor = DeepText
)

