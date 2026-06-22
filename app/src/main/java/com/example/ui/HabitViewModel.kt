package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class HabitViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: HabitRepository

    val habitsState: StateFlow<List<Habit>>
    val userStatsState: StateFlow<UserStats>
    val selectedHabitHistory: MutableStateFlow<List<String>> = MutableStateFlow(emptyList())

    private val _selectedHabit = MutableStateFlow<Habit?>(null)
    val selectedHabit: StateFlow<Habit?> = _selectedHabit.asStateFlow()

    init {
        val database = HabitDatabase.getDatabase(application)
        repository = HabitRepository(database.habitDao)

        // Prep presets
        viewModelScope.launch {
            repository.prepopulatePresets()
        }

        habitsState = repository.allHabits
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        userStatsState = repository.userStats
            .map { it ?: UserStats(id = 1, level = 1, xp = 0, totalCompleted = 0) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = UserStats(id = 1, level = 1, xp = 0, totalCompleted = 0)
            )
    }

    fun selectHabit(habit: Habit) {
        _selectedHabit.value = habit
        viewModelScope.launch {
            repository.getHistoryForHabit(habit.id).collect { history ->
                selectedHabitHistory.value = history.map { it.completedDate }
            }
        }
    }

    fun selectHabitById(id: Int) {
        viewModelScope.launch {
            habitsState.value.find { it.id == id }?.let {
                selectHabit(it)
            }
        }
    }

    fun toggleHabit(habit: Habit) {
        viewModelScope.launch {
            val todayDate = repository.getTodayDateString()
            repository.toggleHabitCompletion(habit, todayDate)
            
            // If the selected habit is the one being toggled, update its stats
            if (_selectedHabit.value?.id == habit.id) {
                // Fetch updated list and refresh the selected product
                habitsState.value.find { it.id == habit.id }?.let {
                    _selectedHabit.value = it
                }
            }
        }
    }

    fun addNewHabit(title: String, description: String, difficulty: String, iconName: String) {
        viewModelScope.launch {
            val xpReward = when (difficulty) {
                "Easy" -> 15
                "Hard" -> 35
                else -> 25 // Medium
            }
            val newHabit = Habit(
                title = title,
                description = description,
                difficulty = difficulty,
                xpReward = xpReward,
                iconName = iconName,
                streakCount = 0,
                isCompletedToday = false
            )
            repository.createHabit(newHabit)
        }
    }

    fun deleteHabit(habit: Habit) {
        viewModelScope.launch {
            repository.deleteHabit(habit)
            if (_selectedHabit.value?.id == habit.id) {
                _selectedHabit.value = null
            }
        }
    }

    fun getXpThreshold(level: Int): Int {
        return repository.xpThresholdForLevel(level)
    }
}
