package com.example.data.repository

import com.example.data.db.DecisionDao
import com.example.data.db.MinuteDao
import com.example.data.db.ReminderDao
import com.example.data.model.DecisionEntity
import com.example.data.model.DecisionStatus
import com.example.data.model.ExtractedAtaResult
import com.example.data.model.MinuteEntity
import com.example.data.model.ReminderEntity
import com.example.util.DateUtils
import kotlinx.coroutines.flow.Flow

class AtaRepository(
    private val minuteDao: MinuteDao,
    private val decisionDao: DecisionDao,
    private val reminderDao: ReminderDao
) {
    val allMinutes: Flow<List<MinuteEntity>> = minuteDao.getAllMinutes()
    val allDecisions: Flow<List<DecisionEntity>> = decisionDao.getAllDecisions()
    val pendingDecisions: Flow<List<DecisionEntity>> = decisionDao.getPendingDecisions()
    val allReminders: Flow<List<ReminderEntity>> = reminderDao.getAllReminders()
    val activeReminders: Flow<List<ReminderEntity>> = reminderDao.getActiveReminders()

    fun getDecisionsForMinute(minuteId: Long): Flow<List<DecisionEntity>> =
        decisionDao.getDecisionsForMinute(minuteId)

    suspend fun getMinutesCount(): Int = minuteDao.getMinutesCount()

    suspend fun saveParsedAta(extracted: ExtractedAtaResult): Long {
        val minute = MinuteEntity(
            title = extracted.title,
            meetingDate = extracted.meetingDateStr,
            originalText = extracted.rawText,
            topicsSummary = extracted.topics.joinToString(" • "),
            extraNotes = extracted.extraTopics,
            attendees = extracted.attendees.joinToString(", ")
        )
        val minuteId = minuteDao.insertMinute(minute)

        val decisions = extracted.decisions.map { item ->
            val dueMillis = when {
                item.decisionType == "PERMANENT_PROCEDURE" -> System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 365 * 10) // 10 years ahead, never overdue
                item.deadlineDateStr.isNotBlank() && !item.deadlineDateStr.contains("Não definido", ignoreCase = true) && !item.deadlineDateStr.contains("Permanente", ignoreCase = true) -> {
                    DateUtils.parseDateTextToMillis(item.deadlineDateStr)
                        ?: DateUtils.addDaysToCurrentTime(if (item.deadlineDays > 0) item.deadlineDays else 15)
                }
                else -> {
                    val days = if (item.deadlineDays > 0) item.deadlineDays else 15
                    DateUtils.addDaysToCurrentTime(days)
                }
            }

            val deadlineDesc = when {
                item.decisionType == "PERMANENT_PROCEDURE" -> "Permanente / Procedimento Contínuo"
                item.deadlineDateStr.isNotBlank() -> item.deadlineDateStr
                item.decisionType == "FOLLOW_UP_ASSIGNMENT" -> "Não definido (15 dias padrão: ${DateUtils.formatMillisToDate(dueMillis)})"
                else -> "Prazo: ${item.deadlineDays} dias (${DateUtils.formatMillisToDate(dueMillis)})"
            }

            DecisionEntity(
                minuteId = minuteId,
                topicTitle = item.topic.ifBlank { extracted.topics.firstOrNull() ?: "Ata de Reunião" },
                description = item.description,
                assignees = item.assignees.joinToString(", "),
                dueDateMillis = dueMillis,
                deadlineDescription = deadlineDesc,
                status = DecisionStatus.PENDING,
                priority = item.priority,
                decisionType = item.decisionType,
                typeLabel = item.typeLabel
            )
        }

        if (decisions.isNotEmpty()) {
            decisionDao.insertDecisions(decisions)
        }

        return minuteId
    }

    suspend fun insertDecision(decision: DecisionEntity): Long = decisionDao.insertDecision(decision)

    suspend fun updateDecision(decision: DecisionEntity) = decisionDao.updateDecision(decision)

    suspend fun updateDecisionStatus(id: Long, status: DecisionStatus) {
        val completedAt = if (status == DecisionStatus.COMPLETED) System.currentTimeMillis() else null
        decisionDao.updateStatus(id, status, completedAt)
    }

    suspend fun deleteDecision(id: Long) = decisionDao.deleteDecisionById(id)

    suspend fun deleteMinute(id: Long) {
        decisionDao.deleteDecisionsForMinute(id)
        minuteDao.deleteMinuteById(id)
    }

    // Reminders
    suspend fun insertReminder(reminder: ReminderEntity): Long = reminderDao.insertReminder(reminder)
    suspend fun updateReminder(reminder: ReminderEntity) = reminderDao.updateReminder(reminder)
    suspend fun toggleReminderCompleted(id: Long, isCompleted: Boolean) = reminderDao.toggleCompleted(id, isCompleted)
    suspend fun deleteReminder(id: Long) = reminderDao.deleteReminderById(id)
}
