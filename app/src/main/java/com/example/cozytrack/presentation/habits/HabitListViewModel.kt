package com.example.cozytrack.presentation.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cozytrack.core.network.ApiResult
import com.example.cozytrack.domain.model.Frequency
import com.example.cozytrack.domain.model.Habit
import com.example.cozytrack.domain.model.HabitProgress
import com.example.cozytrack.domain.model.Thought
import com.example.cozytrack.domain.usecase.auth.GetCurrentUserNameUseCase
import com.example.cozytrack.domain.usecase.auth.LogoutUseCase
import com.example.cozytrack.domain.usecase.habit.CheckHabitUseCase
import com.example.cozytrack.domain.usecase.habit.CreateHabitUseCase
import com.example.cozytrack.domain.usecase.habit.DeleteHabitUseCase
import com.example.cozytrack.domain.usecase.habit.GetEntriesForDayUseCase
import com.example.cozytrack.domain.usecase.habit.GetHabitEntriesUseCase
import com.example.cozytrack.domain.usecase.habit.GetHabitProgressUseCase
import com.example.cozytrack.domain.usecase.habit.GetHabitsUseCase
import com.example.cozytrack.domain.usecase.habit.GetServerClockUseCase
import com.example.cozytrack.domain.usecase.habit.UpdateHabitUseCase
import com.example.cozytrack.domain.usecase.thought.GetThoughtUseCase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HabitListUiState(
    val habits: List<Habit> = emptyList(),
    val progressByHabitId: Map<String, HabitProgress> = emptyMap(),
    val heatmapByHabitId: Map<String, List<HabitHeatmapDay>> = emptyMap(),
    val checkedHabitIds: Set<String> = emptySet(),
    val userName: String = "",
    val thought: Thought? = null,
    val utcCalendarDate: String = "",
    val progressRangeLabel: String = "",
    val newHabitTitle: String = "",
    val newHabitFrequency: Frequency = Frequency.Daily,
    val includeArchived: Boolean = false,
    val editingHabitId: String? = null,
    val editingHabitTitle: String = "",
    val editingHabitFrequency: Frequency = Frequency.Daily,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoggedOut: Boolean = false
)

data class HabitHeatmapDay(
    val calendarDate: String,
    val completed: Boolean,
    val isToday: Boolean
)

class HabitListViewModel(
    private val getServerClockUseCase: GetServerClockUseCase,
    private val getHabitsUseCase: GetHabitsUseCase,
    private val getEntriesForDayUseCase: GetEntriesForDayUseCase,
    private val getHabitEntriesUseCase: GetHabitEntriesUseCase,
    private val createHabitUseCase: CreateHabitUseCase,
    private val updateHabitUseCase: UpdateHabitUseCase,
    private val deleteHabitUseCase: DeleteHabitUseCase,
    private val checkHabitUseCase: CheckHabitUseCase,
    private val getHabitProgressUseCase: GetHabitProgressUseCase,
    private val getThoughtUseCase: GetThoughtUseCase,
    private val getCurrentUserNameUseCase: GetCurrentUserNameUseCase,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(HabitListUiState())
    val uiState: StateFlow<HabitListUiState> = _uiState.asStateFlow()

    init {
        loadHome()
    }

    fun onNewHabitTitleChange(value: String) {
        _uiState.update { it.copy(newHabitTitle = value, errorMessage = null) }
    }

    fun onFrequencyChange(value: Frequency) {
        _uiState.update { it.copy(newHabitFrequency = value) }
    }

    fun onIncludeArchivedChange(value: Boolean) {
        _uiState.update { it.copy(includeArchived = value) }
        loadHome()
    }

    fun startEditingHabit(habit: Habit) {
        _uiState.update {
            it.copy(
                editingHabitId = habit.id,
                editingHabitTitle = habit.title,
                editingHabitFrequency = habit.frequency,
                errorMessage = null
            )
        }
    }

    fun cancelEditingHabit() {
        _uiState.update {
            it.copy(
                editingHabitId = null,
                editingHabitTitle = "",
                editingHabitFrequency = Frequency.Daily,
                errorMessage = null
            )
        }
    }

    fun onEditingHabitTitleChange(value: String) {
        _uiState.update { it.copy(editingHabitTitle = value, errorMessage = null) }
    }

    fun onEditingHabitFrequencyChange(value: Frequency) {
        _uiState.update { it.copy(editingHabitFrequency = value, errorMessage = null) }
    }

    fun saveHabitEdit() {
        val state = _uiState.value
        val habitId = state.editingHabitId ?: return
        if (state.editingHabitTitle.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Habit title is required") }
            return
        }

        viewModelScope.launch {
            when (
                val result = updateHabitUseCase(
                    habitId = habitId,
                    title = state.editingHabitTitle,
                    frequency = state.editingHabitFrequency
                )
            ) {
                is ApiResult.Success -> {
                    cancelEditingHabit()
                    loadHabitsProgressAndEntries()
                }

                is ApiResult.Error -> handleApiError(result.message)
            }
        }
    }

    fun archiveHabit(habit: Habit) {
        setHabitArchived(habit = habit, archived = true)
    }

    fun restoreHabit(habit: Habit) {
        setHabitArchived(habit = habit, archived = false)
    }

    private fun setHabitArchived(habit: Habit, archived: Boolean) {
        viewModelScope.launch {
            when (val result = updateHabitUseCase(habitId = habit.id, isArchived = archived)) {
                is ApiResult.Success -> {
                    loadHabitsProgressAndEntries()
                }

                is ApiResult.Error -> handleApiError(result.message)
            }
        }
    }

    fun deleteHabit(habitId: String) {
        viewModelScope.launch {
            when (val result = deleteHabitUseCase(habitId)) {
                is ApiResult.Success -> {
                    loadHabitsProgressAndEntries()
                }

                is ApiResult.Error -> handleApiError(result.message)
            }
        }
    }

    fun loadHome() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            loadClock()
            loadCurrentUserName()
            loadThought()
            loadHabitsProgressAndEntries()

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun createHabit() {
        val state = _uiState.value
        if (state.newHabitTitle.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Habit title is required") }
            return
        }

        viewModelScope.launch {
            val startDate = state.utcCalendarDate.ifBlank {
                when (val clock = getServerClockUseCase()) {
                    is ApiResult.Success -> clock.data.utcCalendarDate
                    is ApiResult.Error -> {
                        _uiState.update { it.copy(errorMessage = clock.message) }
                        return@launch
                    }
                }
            }

            when (
                val result = createHabitUseCase(
                    title = state.newHabitTitle,
                    frequency = state.newHabitFrequency,
                    startDate = startDate
                )
            ) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(newHabitTitle = "", newHabitFrequency = Frequency.Daily)
                    }
                    loadHabitsProgressAndEntries()
                }

                is ApiResult.Error -> handleApiError(result.message)
            }
        }
    }

    fun onHabitCheckedChange(habitId: String, checked: Boolean) {
        viewModelScope.launch {
            val previousState = _uiState.value

            when (val clock = getServerClockUseCase()) {
                is ApiResult.Success -> {
                    if (clock.data.utcCalendarDate != previousState.utcCalendarDate) {
                        _uiState.update {
                            it.copy(utcCalendarDate = clock.data.utcCalendarDate)
                        }
                        loadHabitsProgressAndEntries()

                        val isAlreadyInRequestedState = (habitId in _uiState.value.checkedHabitIds) == checked
                        if (isAlreadyInRequestedState) return@launch
                    }
                }

                is ApiResult.Error -> {
                    handleApiError(clock.message)
                    return@launch
                }
            }

            val previousCheckedIds = _uiState.value.checkedHabitIds
            _uiState.update {
                it.copy(
                    checkedHabitIds = if (checked) {
                        it.checkedHabitIds + habitId
                    } else {
                        it.checkedHabitIds - habitId
                    },
                    errorMessage = null
                )
            }

            when (val result = checkHabitUseCase(habitId = habitId, completed = checked)) {
                is ApiResult.Success -> {
                    loadHabitsProgressAndEntries()
                }

                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(
                            checkedHabitIds = previousCheckedIds,
                            errorMessage = null
                        )
                    }
                    handleApiError(result.message)
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            logoutUseCase()
            _uiState.update { it.copy(isLoggedOut = true) }
        }
    }

    private suspend fun loadClock() {
        when (val result = getServerClockUseCase()) {
            is ApiResult.Success -> {
                _uiState.update {
                    it.copy(utcCalendarDate = result.data.utcCalendarDate)
                }
            }

            is ApiResult.Error -> {
                _uiState.update { it.copy(errorMessage = result.message) }
            }
        }
    }

    private suspend fun loadThought() {
        when (val result = getThoughtUseCase()) {
            is ApiResult.Success -> {
                _uiState.update { it.copy(thought = result.data) }
            }

            is ApiResult.Error -> {
                // Thought is optional, so do not block the habit screen.
            }
        }
    }

    private suspend fun loadCurrentUserName() {
        when (val result = getCurrentUserNameUseCase()) {
            is ApiResult.Success -> {
                _uiState.update { it.copy(userName = result.data) }
            }

            is ApiResult.Error -> {
                // The name is nice to have; the habit list can still load without it.
            }
        }
    }

    private suspend fun loadHabitsProgressAndEntries() {
        when (val habitsResult = getHabitsUseCase(includeArchived = _uiState.value.includeArchived)) {
            is ApiResult.Success -> {
                val habits = habitsResult.data
                val rangeEnd = _uiState.value.utcCalendarDate
                val rangeStart = rangeEnd.monthStartDateOrNull()
                val heatmapStart = rangeEnd.daysBeforeOrNull(364)
                val progress = habits.mapNotNull { habit ->
                    val result = getHabitProgressUseCase(
                        habitId = habit.id,
                        startDate = heatmapStart ?: rangeStart,
                        endDate = rangeEnd.ifBlank { null }
                    )
                    val progress = (result as? ApiResult.Success)?.data
                    progress?.let { habit.id to it }
                }.toMap()
                val heatmaps = habits.associate { habit ->
                    habit.id to loadHeatmapDays(
                        habitId = habit.id,
                        startDate = heatmapStart,
                        endDate = rangeEnd
                    )
                }
                val checkedHabitIds = loadCheckedHabitIdsForServerDay()

                _uiState.update {
                    it.copy(
                        habits = habits,
                        progressByHabitId = progress,
                        heatmapByHabitId = heatmaps,
                        checkedHabitIds = checkedHabitIds,
                        progressRangeLabel = rangeEnd.monthProgressLabel()
                    )
                }
            }

            is ApiResult.Error -> handleApiError(habitsResult.message)
        }
    }

    private suspend fun loadHeatmapDays(
        habitId: String,
        startDate: String?,
        endDate: String
    ): List<HabitHeatmapDay> {
        if (startDate.isNullOrBlank() || endDate.isBlank()) return emptyList()

        val completedDates = when (
            val entriesResult = getHabitEntriesUseCase(
                habitId = habitId,
                startDate = startDate,
                endDate = endDate
            )
        ) {
            is ApiResult.Success -> entriesResult.data
                .filter { it.completed }
                .map { it.date }
                .toSet()

            is ApiResult.Error -> {
                handleApiError(entriesResult.message)
                emptySet()
            }
        }

        return datesBetween(startDate = startDate, endDate = endDate).map { date ->
            HabitHeatmapDay(
                calendarDate = date,
                completed = date in completedDates,
                isToday = date == endDate
            )
        }
    }

    private suspend fun loadCheckedHabitIdsForServerDay(): Set<String> {
        val checkDate = _uiState.value.utcCalendarDate
        if (checkDate.isBlank()) return emptySet()

        return when (val entriesResult = getEntriesForDayUseCase(checkDate)) {
            is ApiResult.Success -> entriesResult.data
                .filter { it.completed }
                .map { it.habitId }
                .toSet()

            is ApiResult.Error -> {
                handleApiError(entriesResult.message)
                emptySet()
            }
        }
    }

    private suspend fun handleApiError(message: String) {
        if (message.isExpiredTokenError()) {
            logoutUseCase()
            _uiState.update {
                it.copy(
                    habits = emptyList(),
                    progressByHabitId = emptyMap(),
                    heatmapByHabitId = emptyMap(),
                    checkedHabitIds = emptySet(),
                    errorMessage = "Your session expired. Please log in again.",
                    isLoggedOut = true
                )
            }
        } else {
            _uiState.update { it.copy(errorMessage = message) }
        }
    }
}

private fun String.monthStartDateOrNull(): String? {
    return if (length >= 7) "${take(7)}-01" else null
}

private fun String.monthProgressLabel(): String {
    return if (length >= 7) {
        "Month progress (${take(7)}-01 to $this UTC)"
    } else {
        "Month progress"
    }
}

private fun String.daysBeforeOrNull(days: Int): String? {
    val formatter = utcDateFormatter()
    return runCatching {
        val parsedDate = formatter.parse(this) ?: return@runCatching null
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        calendar.time = parsedDate
        calendar.add(Calendar.DAY_OF_YEAR, -days)
        formatter.format(calendar.time)
    }.getOrNull()
}

private fun datesBetween(startDate: String, endDate: String): List<String> {
    val formatter = utcDateFormatter()
    return runCatching {
        val parsedStart = formatter.parse(startDate) ?: return@runCatching emptyList()
        val parsedEnd = formatter.parse(endDate) ?: return@runCatching emptyList()
        val start = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            time = parsedStart
        }
        val end = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            time = parsedEnd
        }
        val dates = mutableListOf<String>()
        while (!start.after(end)) {
            dates += formatter.format(start.time)
            start.add(Calendar.DAY_OF_YEAR, 1)
        }
        dates
    }.getOrDefault(emptyList())
}

private fun utcDateFormatter(): SimpleDateFormat {
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
}

private fun String.isExpiredTokenError(): Boolean {
    val lowerMessage = lowercase(Locale.US)
    return "http 401" in lowerMessage ||
        "invalid or expired access token" in lowerMessage ||
        "unauthorized" in lowerMessage
}
