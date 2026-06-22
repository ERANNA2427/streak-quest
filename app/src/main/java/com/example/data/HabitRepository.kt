package com.example.data

import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*

class HabitRepository(private val habitDao: HabitDao) {

    val allHabits: Flow<List<Habit>> = habitDao.getAllHabitsFlow()
    val allHistory: Flow<List<HabitHistory>> = habitDao.getAllHistoryFlow()
    val userStats: Flow<UserStats?> = habitDao.getUserStatsFlow()

    fun getHistoryForHabit(habitId: Int): Flow<List<HabitHistory>> {
        return habitDao.getHistoryForHabitFlow(habitId)
    }

    suspend fun createHabit(habit: Habit) {
        val id = habitDao.insertHabit(habit)
        // Also pre-populate some history days for visual streak aesthetic immediately!
        // This is excellent for first-run demonstration of the 30-day calendar grid.
        prepopulateSomeHistory(id.toInt())
    }

    suspend fun deleteHabit(habit: Habit) {
        habitDao.clearHistoryForHabit(habit.id)
        habitDao.deleteHabit(habit)
    }

    suspend fun getOrInitUserStats(): UserStats {
        var stats = habitDao.getUserStats()
        if (stats == null) {
            stats = UserStats(id = 1, level = 1, xp = 0, totalCompleted = 0)
            habitDao.saveUserStats(stats)
        }
        return stats
    }

    suspend fun toggleHabitCompletion(habit: Habit, dateString: String) {
        val todayLog = habitDao.getHistoryForHabitOnDate(habit.id, dateString)
        val stats = getOrInitUserStats()

        val isNowCompleted = todayLog == null

        if (isNowCompleted) {
            // MARK COMPLETED
            habitDao.insertHistory(
                HabitHistory(
                    habitId = habit.id,
                    completedDate = dateString,
                    timestamp = System.currentTimeMillis()
                )
            )

            // Dynamic Leveling Up calculation
            var currentLvl = stats.level
            var currentXp = stats.xp + habit.xpReward
            while (currentXp >= xpThresholdForLevel(currentLvl)) {
                currentXp -= xpThresholdForLevel(currentLvl)
                currentLvl += 1
            }

            habitDao.saveUserStats(
                stats.copy(
                    level = currentLvl,
                    xp = currentXp,
                    totalCompleted = stats.totalCompleted + 1
                )
            )

            // Update habit streak and state
            habitDao.updateHabit(
                habit.copy(
                    isCompletedToday = true,
                    streakCount = habit.streakCount + 1
                )
            )
        } else {
            // UN-MARK COMPLETED
            habitDao.deleteHistoryForHabitOnDate(habit.id, dateString)

            var currentXp = stats.xp - habit.xpReward
            var currentLvl = stats.level
            if (currentXp < 0) {
                if (currentLvl > 1) {
                    currentLvl -= 1
                    currentXp += xpThresholdForLevel(currentLvl)
                } else {
                    currentXp = 0
                }
            }

            habitDao.saveUserStats(
                stats.copy(
                    level = currentLvl,
                    xp = currentXp,
                    totalCompleted = (stats.totalCompleted - 1).coerceAtLeast(0)
                )
            )

            habitDao.updateHabit(
                habit.copy(
                    isCompletedToday = false,
                    streakCount = (habit.streakCount - 1).coerceAtLeast(0)
                )
            )
        }
    }

    fun xpThresholdForLevel(level: Int): Int {
        // level 1: 100 XP to reach level 2
        // level 2: 150 XP to reach level 3
        // level 3: 200 XP to reach level 4, etc.
        return 100 + (level - 1) * 50
    }

    suspend fun prepopulatePresets() {
        val habits = habitDao.getAllHabitsFlow()
        // Check if DB already populated
        val existing = habitDao.getUserStats()
        if (existing == null) {
            // Initialize default stats
            habitDao.saveUserStats(UserStats(id = 1, level = 1, xp = 15, totalCompleted = 12))

            // Add some beautiful gamified habit presets
            val preset1Id = habitDao.insertHabit(
                Habit(
                    title = "Water of Youth",
                    description = "Drink 2.5 Liters of pure water.",
                    xpReward = 15,
                    difficulty = "Easy",
                    iconName = "water_drop",
                    streakCount = 5,
                    isCompletedToday = false
                )
            )
            prepopulateSomeHistoryWithStreak(preset1Id.toInt(), 5)

            val preset2Id = habitDao.insertHabit(
                Habit(
                    title = "Shadow Slayer Gym Workout",
                    description = "Strength training or cardio for 45 mins.",
                    xpReward = 35,
                    difficulty = "Hard",
                    iconName = "fitness_center",
                    streakCount = 3,
                    isCompletedToday = true
                )
            )
            prepopulateSomeHistoryWithStreak(preset2Id.toInt(), 3)
            // also log completion for today
            val todayDate = getTodayDateString()
            habitDao.insertHistory(HabitHistory(habitId = preset2Id.toInt(), completedDate = todayDate))

            val preset3Id = habitDao.insertHabit(
                Habit(
                    title = "Sorcerer's Focus",
                    description = "Study coding or read non-fiction for 1 hr.",
                    xpReward = 25,
                    difficulty = "Medium",
                    iconName = "book",
                    streakCount = 12,
                    isCompletedToday = false
                )
            )
            prepopulateSomeHistoryWithStreak(preset3Id.toInt(), 12)

            val preset4Id = habitDao.insertHabit(
                Habit(
                    title = "Ethereal Sleep Routine",
                    description = "No screens 30 mins before sleep. Sleep by 11 PM.",
                    xpReward = 20,
                    difficulty = "Easy",
                    iconName = "bedtime",
                    streakCount = 8,
                    isCompletedToday = true
                )
            )
            prepopulateSomeHistoryWithStreak(preset4Id.toInt(), 8)
            habitDao.insertHistory(HabitHistory(habitId = preset4Id.toInt(), completedDate = todayDate))
        }
    }

    private suspend fun prepopulateSomeHistory(habitId: Int) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        // Randomly complete 5 of the last 15 days
        val rand = Random()
        for (i in 1..15) {
            if (rand.nextBoolean()) {
                cal.time = Date()
                cal.add(Calendar.DAY_OF_YEAR, -i)
                val dateString = sdf.format(cal.time)
                habitDao.insertHistory(HabitHistory(habitId = habitId, completedDate = dateString))
            }
        }
    }

    private suspend fun prepopulateSomeHistoryWithStreak(habitId: Int, streakDays: Int) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        // Ensure consecutive streak days starting from yesterday backwards
        for (i in 1..streakDays) {
            cal.time = Date()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            val dateString = sdf.format(cal.time)
            habitDao.insertHistory(HabitHistory(habitId = habitId, completedDate = dateString))
        }

        // Add 5 more random completions further back from days 15..30
        val rand = Random()
        for (i in (streakDays + 2)..28) {
            if (rand.nextInt(100) < 40) { // 40% probability
                cal.time = Date()
                cal.add(Calendar.DAY_OF_YEAR, -i)
                val dateString = sdf.format(cal.time)
                habitDao.insertHistory(HabitHistory(habitId = habitId, completedDate = dateString))
            }
        }
    }

    fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }
}
