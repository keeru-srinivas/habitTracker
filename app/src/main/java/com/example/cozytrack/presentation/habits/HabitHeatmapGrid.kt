package com.example.cozytrack.presentation.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.cozytrack.presentation.components.BrownPrimary
import com.example.cozytrack.presentation.components.DeepText
import com.example.cozytrack.presentation.components.DeleteRed
import com.example.cozytrack.presentation.components.GreenDone
import com.example.cozytrack.presentation.components.MutedText
import com.example.cozytrack.presentation.components.PinkToday
import com.example.cozytrack.presentation.components.prettyDate
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

private const val DAYS_PER_WEEK = 7
private val cellSize = 11.dp
private val cellGap = 3.dp
private val dayLabelWidth = 30.dp
private val monthRowHeight = 16.dp
private val gridTopGap = 4.dp

private data class MonthMarker(
    val columnIndex: Int,
    val label: String
)

private data class GitHubHeatmapLayout(
    val weekColumns: List<List<HabitHeatmapDay?>>,
    val monthMarkers: List<MonthMarker>
)

@Composable
fun HabitHeatmap(days: List<HabitHeatmapDay>) {
    val layout = remember(days) { buildGitHubHeatmapLayout(days) }
    var selectedDay by remember(days) {
        mutableStateOf(days.lastOrNull { it.isToday } ?: days.lastOrNull())
    }
    val scrollState = rememberScrollState()
    val gridWidth = gridWidth(layout.weekColumns.size)

    LaunchedEffect(layout.weekColumns.size, scrollState.maxValue) {
        if (scrollState.maxValue > 0) {
            scrollState.scrollTo(scrollState.maxValue)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        days.lastOrNull { it.isToday }?.let { today ->
            Text(
                text = buildString {
                    append("Today · ")
                    append(today.calendarDate.prettyDate())
                    append(" · ")
                    append(dayOfWeekLabel(today.calendarDate))
                },
                color = DeepText,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.labelMedium
            )
        }

        Row(
            modifier = Modifier.padding(vertical = 2.dp),
            verticalAlignment = Alignment.Top
        ) {
            DayOfWeekLabels()

            Column(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(scrollState)
            ) {
                MonthLabelRow(
                    gridWidth = gridWidth,
                    monthMarkers = layout.monthMarkers
                )

                Spacer(modifier = Modifier.height(gridTopGap))

                GitHubGrid(
                    gridWidth = gridWidth,
                    weekColumns = layout.weekColumns,
                    selectedDay = selectedDay,
                    onDaySelected = { selectedDay = it }
                )
            }
        }

        selectedDay?.let { day ->
            Text(
                text = buildString {
                    append(day.calendarDate.prettyDate())
                    append(" · ")
                    append(dayOfWeekLabel(day.calendarDate))
                    append(" · ")
                    append(if (day.completed) "Completed" else "Not completed")
                    if (day.isToday) append(" (today)")
                },
                color = if (day.completed) GreenDone else DeepText,
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.labelMedium
            )
        }

        HeatmapLegend()
    }
}

@Composable
private fun DayOfWeekLabels() {
    val labels = mapOf(
        1 to "Mon",
        3 to "Wed",
        5 to "Fri"
    )

    Column(
        modifier = Modifier.width(dayLabelWidth),
        verticalArrangement = Arrangement.spacedBy(cellGap)
    ) {
        Spacer(modifier = Modifier.height(monthRowHeight + gridTopGap))

        repeat(DAYS_PER_WEEK) { rowIndex ->
            Box(
                modifier = Modifier.size(width = dayLabelWidth, height = cellSize),
                contentAlignment = Alignment.CenterStart
            ) {
                labels[rowIndex]?.let { label ->
                    Text(
                        text = label,
                        color = MutedText,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Start
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthLabelRow(
    gridWidth: Dp,
    monthMarkers: List<MonthMarker>
) {
    Box(
        modifier = Modifier
            .width(gridWidth)
            .height(monthRowHeight)
    ) {
        monthMarkers.forEach { marker ->
            Text(
                text = marker.label,
                modifier = Modifier.offset(x = columnOffset(marker.columnIndex)),
                color = MutedText,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun GitHubGrid(
    gridWidth: Dp,
    weekColumns: List<List<HabitHeatmapDay?>>,
    selectedDay: HabitHeatmapDay?,
    onDaySelected: (HabitHeatmapDay) -> Unit
) {
    Column(
        modifier = Modifier.width(gridWidth),
        verticalArrangement = Arrangement.spacedBy(cellGap)
    ) {
        repeat(DAYS_PER_WEEK) { rowIndex ->
            Row(horizontalArrangement = Arrangement.spacedBy(cellGap)) {
                weekColumns.forEach { column ->
                    val day = column[rowIndex]
                    if (day == null) {
                        Box(modifier = Modifier.size(cellSize))
                    } else {
                        val isSelected = selectedDay?.calendarDate == day.calendarDate
                        Box(
                            modifier = Modifier
                                .size(cellSize)
                                .background(
                                    color = heatmapColor(day),
                                    shape = RoundedCornerShape(3.dp)
                                )
                                .border(
                                    width = when {
                                        isSelected -> 1.5.dp
                                        day.isToday -> 1.5.dp
                                        else -> 0.dp
                                    },
                                    color = when {
                                        isSelected -> BrownPrimary
                                        day.isToday -> PinkToday
                                        else -> Color.Transparent
                                    },
                                    shape = RoundedCornerShape(3.dp)
                                )
                                .clickable { onDaySelected(day) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeatmapLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Less",
            color = MutedText,
            style = MaterialTheme.typography.labelSmall
        )
        Spacer(modifier = Modifier.width(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            listOf(
                Color(0xFFF4E8D7),
                Color(0xFFE8C7A3),
                BrownPrimary,
                DeleteRed
            ).forEach { color ->
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(color = color, shape = RoundedCornerShape(2.dp))
                )
            }
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "More",
            color = MutedText,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

private fun columnOffset(columnIndex: Int): Dp {
    return (cellSize + cellGap) * columnIndex
}

private fun gridWidth(weekCount: Int): Dp {
    if (weekCount <= 0) return 0.dp
    return cellSize * weekCount + cellGap * (weekCount - 1)
}

private fun heatmapColor(day: HabitHeatmapDay): Color {
    return when {
        day.completed && day.isToday -> DeleteRed
        day.completed -> BrownPrimary
        day.isToday -> Color(0xFFE8C7A3)
        else -> Color(0xFFF4E8D7)
    }
}

private fun buildGitHubHeatmapLayout(days: List<HabitHeatmapDay>): GitHubHeatmapLayout {
    if (days.isEmpty()) {
        return GitHubHeatmapLayout(weekColumns = emptyList(), monthMarkers = emptyList())
    }

    val formatter = utcDateFormatter()
    val dayByDate = days.associateBy { it.calendarDate }
    val rangeStart = formatter.parse(days.first().calendarDate)
        ?: return GitHubHeatmapLayout(emptyList(), emptyList())
    val rangeEnd = formatter.parse(days.last().calendarDate)
        ?: return GitHubHeatmapLayout(emptyList(), emptyList())

    val gridStart = startOfWeekSunday(rangeStart)
    val weekColumns = mutableListOf<List<HabitHeatmapDay?>>()
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    calendar.time = gridStart

    while (!calendar.time.after(rangeEnd)) {
        val column = mutableListOf<HabitHeatmapDay?>()

        repeat(DAYS_PER_WEEK) {
            val dateString = formatter.format(calendar.time)
            val inRange = !calendar.time.before(rangeStart) && !calendar.time.after(rangeEnd)
            column += if (inRange) dayByDate[dateString] else null
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        weekColumns += column
    }

    val monthMarkers = buildMonthMarkers(weekColumns)

    return GitHubHeatmapLayout(
        weekColumns = weekColumns,
        monthMarkers = monthMarkers
    )
}

private fun buildMonthMarkers(weekColumns: List<List<HabitHeatmapDay?>>): List<MonthMarker> {
    val markers = mutableListOf<MonthMarker>()
    var previousMonthKey: Int? = null

    weekColumns.forEachIndexed { columnIndex, column ->
        val firstDayInColumn = column.firstNotNullOfOrNull { it } ?: return@forEachIndexed
        val monthKey = monthKey(firstDayInColumn.calendarDate)
        if (monthKey != previousMonthKey) {
            markers += MonthMarker(
                columnIndex = columnIndex,
                label = monthAbbrevFromDate(firstDayInColumn.calendarDate)
            )
            previousMonthKey = monthKey
        }
    }

    return markers
}

private fun monthKey(calendarDate: String): Int {
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    calendar.time = utcDateFormatter().parse(calendarDate) ?: return -1
    return calendar.get(Calendar.YEAR) * 12 + calendar.get(Calendar.MONTH)
}

private fun monthAbbrevFromDate(calendarDate: String): String {
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    calendar.time = utcDateFormatter().parse(calendarDate) ?: return ""
    return monthAbbrev(calendar.get(Calendar.MONTH))
}

private fun startOfWeekSunday(date: java.util.Date): java.util.Date {
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    calendar.time = date
    val dayOffset = calendar.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
    calendar.add(Calendar.DAY_OF_YEAR, -dayOffset)
    return calendar.time
}

private fun dayOfWeekLabel(calendarDate: String): String {
    return runCatching {
        val formatter = utcDateFormatter()
        val parsed = formatter.parse(calendarDate) ?: return@runCatching calendarDate
        val output = SimpleDateFormat("EEEE", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        output.format(parsed)
    }.getOrDefault(calendarDate)
}

private fun monthAbbrev(monthIndex: Int): String {
    return SimpleDateFormat("MMM", Locale.US).format(
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(Calendar.MONTH, monthIndex)
            set(Calendar.DAY_OF_MONTH, 1)
        }.time
    )
}

private fun utcDateFormatter(): SimpleDateFormat {
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
}
