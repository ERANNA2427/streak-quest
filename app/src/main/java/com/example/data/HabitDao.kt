package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {

    // --- Habit Queries ---
    @Query("SELECT * FROM habits ORDER BY createdAt DESC")
    fun getAllHabitsFlow(): Flow<List<Habit>>

    @Query("SELECT * FROM habits WHERE id = :id")
    fun getHabitByIdFlow(id: Int): Flow<Habit?>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getHabitById(id: Int): Habit?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: Habit): Long

    @Update
    suspend fun updateHabit(habit: Habit)

    @Delete
    suspend fun deleteHabit(habit: Habit)

    // --- Habit Completed History ---
    @Query("SELECT * FROM habit_history ORDER BY timestamp DESC")
    fun getAllHistoryFlow(): Flow<List<HabitHistory>>

    @Query("SELECT * FROM habit_history WHERE habitId = :habitId ORDER BY completedDate DESC")
    fun getHistoryForHabitFlow(habitId: Int): Flow<List<HabitHistory>>

    @Query("SELECT * FROM habit_history WHERE habitId = :habitId AND completedDate = :date LIMIT 1")
    suspend fun getHistoryForHabitOnDate(habitId: Int, date: String): HabitHistory?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: HabitHistory)

    @Query("DELETE FROM habit_history WHERE habitId = :habitId AND completedDate = :date")
    suspend fun deleteHistoryForHabitOnDate(habitId: Int, date: String)

    @Query("DELETE FROM habit_history WHERE habitId = :habitId")
    suspend fun clearHistoryForHabit(habitId: Int)

    // --- User Stats ---
    @Query("SELECT * FROM user_stats WHERE id = 1")
    fun getUserStatsFlow(): Flow<UserStats?>

    @Query("SELECT * FROM user_stats WHERE id = 1")
    suspend fun getUserStats(): UserStats?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserStats(stats: UserStats)
}
