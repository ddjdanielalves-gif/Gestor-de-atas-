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

    val allMinutes: Flow<List<MinuteEntity>> =
        minuteDao.getAllMinutes()

    val allDecisions: Flow<List<DecisionEntity>> =
        decisionDao.getAllDecisions()

    val pendingDecisions: Flow<List<DecisionEntity>> =
        decisionDao.getPendingDecisions()

    val allReminders: Flow<List<ReminderEntity>> =
        reminderDao.getAllReminders()

    val activeReminders: Flow<List<ReminderEntity>> =
        reminderDao.getActiveReminders()

    fun getDecisionsForMinute(
        minuteId: Long
    ): Flow<List<DecisionEntity>> =
        decisionDao.getDecisionsForMinute(minuteId)

    suspend fun getMinutesCount(): Int =
        minuteDao.getMinutesCount()

    /**
     * Salva uma ata interpretada pela IA.
     *
     * REGRA PRINCIPAL DE PRAZO:
     *
     * 1. Se a decisão tiver uma data explícita,
     *    usa essa data.
     *
     * 2. Se tiver quantidade de dias,
     *    calcula a partir da DATA DA ATA.
     *
     * 3. Se não houver prazo,
     *    aplica 15 dias a partir da DATA DA ATA.
     *
     * 4. Procedimento permanente não possui
     *    vencimento real.
     */
    suspend fun saveParsedAta(
        extracted: ExtractedAtaResult
    ): Long {

        val minute = MinuteEntity(
            title = extracted.title,
            meetingDate = extracted.meetingDateStr,
            originalText = extracted.rawText,
            topicsSummary =
                extracted.topics.joinToString(" • "),
            extraNotes = extracted.extraTopics,
            attendees =
                extracted.attendees.joinToString(", ")
        )

        val minuteId =
            minuteDao.insertMinute(minute)

        /*
         * Tenta descobrir a data da reunião.
         *
         * Primeiro usa a data identificada pela IA.
         * Se não conseguir, procura uma data no texto original.
         */
        val meetingDateMillis =
            DateUtils.parseDateTextToMillis(
                extracted.meetingDateStr
            )
                ?: extractFirstDateFromText(
                    extracted.rawText
                )?.let {
                    DateUtils.parseDateTextToMillis(it)
                }

        val decisions =
            extracted.decisions.map { item ->

                val dueMillis =
                    calculateDueDate(
                        item = item,
                        meetingDateMillis =
                            meetingDateMillis
                    )

                val deadlineDescription =
                    buildDeadlineDescription(
                        item = item,
                        dueMillis = dueMillis
                    )

                DecisionEntity(
                    minuteId = minuteId,

                    topicTitle =
                        item.topic.ifBlank {
                            extracted.topics
                                .firstOrNull()
                                ?: "Ata de Reunião"
                        },

                    description =
                        item.description,

                    /*
                     * Se a IA não encontrou responsável,
                     * fica vazio.
                     *
                     * NÃO inventamos nomes.
                     */
                    assignees =
                        item.assignees
                            .joinToString(", "),

                    dueDateMillis =
                        dueMillis,

                    deadlineDescription =
                        deadlineDescription,

                    status =
                        DecisionStatus.PENDING,

                    priority =
                        item.priority,

                    decisionType =
                        item.decisionType,

                    typeLabel =
                        item.typeLabel
                )
            }

        if (decisions.isNotEmpty()) {
            decisionDao.insertDecisions(
                decisions
            )
        }

        return minuteId
    }

    /**
     * Calcula o vencimento.
     */
    private fun calculateDueDate(
        item:
            com.example.data.model.ExtractedDecisionItem,

        meetingDateMillis: Long?
    ): Long {

        /*
         * Procedimento permanente.
         *
         * O banco atual exige um Long para dueDateMillis.
         * Portanto usamos uma data distante como representação
         * técnica de "sem vencimento".
         */
        if (
            item.decisionType ==
                "PERMANENT_PROCEDURE"
        ) {
            return permanentDateMillis()
        }

        /*
         * Se a IA identificou uma data específica,
         * ela tem prioridade sobre quantidade de dias.
         */
        val explicitDate =
            DateUtils.parseDateTextToMillis(
                item.deadlineDateStr
            )

        if (explicitDate != null) {
            return explicitDate
        }

        /*
         * DATA BASE:
         *
         * A data da ata.
         *
         * Nunca usamos a data atual aqui quando a data
         * da reunião está disponível.
         */
        val baseDate =
            meetingDateMillis
                ?: DateUtils.getStartOfDay()

        /*
         * Se não houver prazo, 15 dias.
         */
        val days =
            if (item.deadlineDays > 0) {
                item.deadlineDays
            } else {
                15
            }

        return DateUtils.addDaysToDate(
            baseMillis = baseDate,
            days = days
        )
    }

    /**
     * Cria o texto que aparece na interface.
     */
    private fun buildDeadlineDescription(
        item:
            com.example.data.model.ExtractedDecisionItem,

        dueMillis: Long
    ): String {

        if (
            item.decisionType ==
                "PERMANENT_PROCEDURE"
        ) {
            return "Permanente / Procedimento Contínuo"
        }

        val explicitDate =
            DateUtils.parseDateTextToMillis(
                item.deadlineDateStr
            )

        if (explicitDate != null) {
            return "Vencimento: ${
                DateUtils.formatMillisToDate(
                    explicitDate
                )
            }"
        }

        val days =
            if (item.deadlineDays > 0) {
                item.deadlineDays
            } else {
                15
            }

        val wasDefault =
            !item.deadlineProvided &&
                days == 15

        return if (wasDefault) {

            "Prazo padrão: 15 dias — " +
                "vence em ${
                    DateUtils.formatMillisToDate(
                        dueMillis
                    )
                }"

        } else {

            "Prazo: $days dias — " +
                "vence em ${
                    DateUtils.formatMillisToDate(
                        dueMillis
                    )
                }"
        }
    }

    /**
     * Procura a primeira data no texto da ata.
     */
    private fun extractFirstDateFromText(
        text: String
    ): String? {

        val regex =
            Regex(
                "(\\d{1,2}/\\d{1,2}/\\d{4})" +
                    "|" +
                    "(\\d{4}-\\d{2}-\\d{2})" +
                    "|" +
                    "(\\d{1,2}\\s+de\\s+" +
                    "[A-Za-zÀ-ÿ]+" +
                    "\\s+de\\s+\\d{4})",
                RegexOption.IGNORE_CASE
            )

        return regex
            .find(text)
            ?.value
    }

    /**
     * Data técnica para decisões permanentes.
     */
    private fun permanentDateMillis(): Long {

        return DateUtils
            .parseDateTextToMillis(
                "31/12/2099"
            )
            ?: DateUtils.addDaysToDate(
                DateUtils.getStartOfDay(),
                365 * 100
            )
    }

    suspend fun insertDecision(
        decision: DecisionEntity
    ): Long =
        decisionDao.insertDecision(
            decision
        )

    suspend fun updateDecision(
        decision: DecisionEntity
    ) =
        decisionDao.updateDecision(
            decision
        )

    suspend fun updateDecisionStatus(
        id: Long,
        status: DecisionStatus
    ) {

        val completedAt =
            if (
                status ==
                    DecisionStatus.COMPLETED
            ) {
                System.currentTimeMillis()
            } else {
                null
            }

        decisionDao.updateStatus(
            id,
            status,
            completedAt
        )
    }

    suspend fun deleteDecision(
        id: Long
    ) =
        decisionDao.deleteDecisionById(id)

    suspend fun deleteMinute(
        id: Long
    ) {

        decisionDao
            .deleteDecisionsForMinute(id)

        minuteDao
            .deleteMinuteById(id)
    }

    // ---------------------------------------------------------
    // REMINDERS
    // ---------------------------------------------------------

    suspend fun insertReminder(
        reminder: ReminderEntity
    ): Long =
        reminderDao.insertReminder(
            reminder
        )

    suspend fun updateReminder(
        reminder: ReminderEntity
    ) =
        reminderDao.updateReminder(
            reminder
        )

    suspend fun toggleReminderCompleted(
        id: Long,
        isCompleted: Boolean
    ) =
        reminderDao.toggleCompleted(
            id,
            isCompleted
        )

    suspend fun deleteReminder(
        id: Long
    ) =
        reminderDao.deleteReminderById(id)
}
