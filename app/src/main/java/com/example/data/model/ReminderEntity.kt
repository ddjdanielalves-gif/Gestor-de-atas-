package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val reminderDateMillis: Long, // Due date in millis
    val reminderTime: String = "09:00", // e.g. "09:00", "15:30"
    val category: String = "Geral", // "Reunião", "Prazo de Ata", "Acompanhamento", "Geral"
    val isCompleted: Boolean = false,
    val isPriority: Boolean = false,
    val relatedDecisionId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
