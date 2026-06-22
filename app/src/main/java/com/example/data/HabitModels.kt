package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val xpReward: Int = 20,
    val difficulty: String = "Medium", // "Easy", "Medium", "Hard"
    val iconName: String = "Star", // Material icon label
    val streakCount: Int = 0,
    val isCompletedToday: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "habit_history")
data class HabitHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val habitId: Int,
    val completedDate: String, // String format: "yyyy-MM-dd"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_stats")
data class UserStats(
    @PrimaryKey val id: Int = 1, // Singleton row
    val level: Int = 1,
    val xp: Int = 0,
    val totalCompleted: Int = 0
)
