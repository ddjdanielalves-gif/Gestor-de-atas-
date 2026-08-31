package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class DecisionStatus {
    PENDING,      // Pendente
    IN_PROGRESS,  // Em Andamento
    COMPLETED     // Concluída
}

enum class PriorityLevel {
    NORMAL,
    HIGH,
    URGENT
}

@Entity(tableName = "decisions")
data class DecisionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val minuteId: Long? = null,
    val topicTitle: String = "",
    val description: String,
    val assignees: String, // Comma-separated names, e.g. "Edvaldo, Reginaldo"
    val dueDateMillis: Long, // Epoch timestamp in milliseconds for deadline
    val deadlineDescription: String = "", // e.g. "Não definido (15 dias padrão)"
    val status: DecisionStatus = DecisionStatus.PENDING,
    val priority: PriorityLevel = PriorityLevel.NORMAL,
    val decisionType: String = "FOLLOW_UP_ASSIGNMENT", // "FOLLOW_UP_ASSIGNMENT", "PERMANENT_PROCEDURE", "ACTION_DEADLINE"
    val typeLabel: String = "Atribuição de acompanhamento",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)
