package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.DecisionEntity
import com.example.data.model.DecisionStatus
import com.example.data.model.ExtractedAtaResult
import com.example.data.model.MinuteEntity
import com.example.data.model.PriorityLevel
import com.example.data.model.ReminderEntity
import com.example.data.repository.AtaRepository
import com.example.service.AtaParserService
import com.example.util.DateUtils
import com.example.util.DeadlineUrgency
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardAlertsSummary(
    val overdueCount: Int = 0,
    val dueTodayCount: Int = 0,
    val dueSoonCount: Int = 0,
    val pendingRemindersTodayCount: Int = 0,
    val totalPendingDecisions: Int = 0,
    val totalCompletedDecisions: Int = 0
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AtaRepository
    private val parserService = AtaParserService()

    val allMinutes: StateFlow<List<MinuteEntity>>
    val allDecisions: StateFlow<List<DecisionEntity>>
    val allReminders: StateFlow<List<ReminderEntity>>
    val activeReminders: StateFlow<List<ReminderEntity>>

    private val _isProcessingAta = MutableStateFlow(false)
    val isProcessingAta: StateFlow<Boolean> = _isProcessingAta.asStateFlow()

    private val _lastExtractedAta = MutableStateFlow<ExtractedAtaResult?>(null)
    val lastExtractedAta: StateFlow<ExtractedAtaResult?> = _lastExtractedAta.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedAssigneeFilter = MutableStateFlow<String?>(null)
    val selectedAssigneeFilter: StateFlow<String?> = _selectedAssigneeFilter.asStateFlow()

    private val _selectedStatusFilter = MutableStateFlow<String>("TODAS") // "TODAS", "ATRASADAS", "HOJE", "EM_BREVE", "CONCLUIDAS"
    val selectedStatusFilter: StateFlow<String> = _selectedStatusFilter.asStateFlow()

    private val _userConsultationNotice = MutableStateFlow<String?>(null)
    val userConsultationNotice: StateFlow<String?> = _userConsultationNotice.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = AtaRepository(db.minuteDao(), db.decisionDao(), db.reminderDao())

        allMinutes = repository.allMinutes
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allDecisions = repository.allDecisions
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allReminders = repository.allReminders
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        activeReminders = repository.activeReminders
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        preloadInitialDataIfEmpty()
    }

    val alertsSummary: StateFlow<DashboardAlertsSummary> = combine(
        allDecisions,
        allReminders
    ) { decisions, reminders ->
        var overdue = 0
        var dueToday = 0
        var dueSoon = 0
        var completed = 0
        var pending = 0

        val startOfToday = DateUtils.getStartOfDay()
        val endOfToday = DateUtils.getEndOfDay()

        for (dec in decisions) {
            if (dec.status == DecisionStatus.COMPLETED) {
                completed++
            } else {
                pending++
                when (DateUtils.getUrgency(dec.dueDateMillis, false)) {
                    DeadlineUrgency.OVERDUE -> overdue++
                    DeadlineUrgency.TODAY -> dueToday++
                    DeadlineUrgency.SOON -> dueSoon++
                    else -> {}
                }
            }
        }

        val remindersToday = reminders.count {
            !it.isCompleted && it.reminderDateMillis in startOfToday..endOfToday
        }

        DashboardAlertsSummary(
            overdueCount = overdue,
            dueTodayCount = dueToday,
            dueSoonCount = dueSoon,
            pendingRemindersTodayCount = remindersToday,
            totalPendingDecisions = pending,
            totalCompletedDecisions = completed
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardAlertsSummary())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setAssigneeFilter(assignee: String?) {
        _selectedAssigneeFilter.value = assignee
    }

    fun setStatusFilter(status: String) {
        _selectedStatusFilter.value = status
    }

    fun clearExtractedAta() {
        _lastExtractedAta.value = null
    }

    fun dismissNotice() {
        _userConsultationNotice.value = null
    }

    fun parseAndProcessAta(text: String, onFinished: (Boolean) -> Unit = {}) {
        if (text.isBlank()) {
            onFinished(false)
            return
        }
        viewModelScope.launch {
            _isProcessingAta.value = true
            try {
                val result = parserService.parseMinuteText(text)
                _lastExtractedAta.value = result
                onFinished(true)
            } catch (_: Exception) {
                // Fallback to local parsing
                val fallback = parserService.parseLocalMinuteText(text)
                _lastExtractedAta.value = fallback
                onFinished(true)
            } finally {
                _isProcessingAta.value = false
            }
        }
    }

    fun saveExtractedAta(extracted: ExtractedAtaResult, onSaved: () -> Unit) {
        viewModelScope.launch {
            repository.saveParsedAta(extracted)
            _lastExtractedAta.value = null
            _userConsultationNotice.value = "Ata e ${extracted.decisions.size} decisões registradas com sucesso!"
            onSaved()
        }
    }

    fun updateDecisionStatus(decisionId: Long, newStatus: DecisionStatus) {
        viewModelScope.launch {
            repository.updateDecisionStatus(decisionId, newStatus)
        }
    }

    fun updateDecision(decision: DecisionEntity) {
        viewModelScope.launch {
            repository.updateDecision(decision)
        }
    }

    fun deleteDecision(decisionId: Long) {
        viewModelScope.launch {
            repository.deleteDecision(decisionId)
        }
    }

    fun deleteMinute(minuteId: Long) {
        viewModelScope.launch {
            repository.deleteMinute(minuteId)
        }
    }

    // Reminder Actions
    fun addReminder(
        title: String,
        description: String,
        dateMillis: Long,
        time: String,
        category: String,
        isPriority: Boolean,
        relatedDecisionId: Long? = null
    ) {
        viewModelScope.launch {
            val reminder = ReminderEntity(
                title = title,
                description = description,
                reminderDateMillis = dateMillis,
                reminderTime = time,
                category = category,
                isPriority = isPriority,
                relatedDecisionId = relatedDecisionId
            )
            repository.insertReminder(reminder)
        }
    }

    fun toggleReminderCompleted(reminderId: Long, isCompleted: Boolean) {
        viewModelScope.launch {
            repository.toggleReminderCompleted(reminderId, isCompleted)
        }
    }

    fun deleteReminder(reminderId: Long) {
        viewModelScope.launch {
            repository.deleteReminder(reminderId)
        }
    }

    private fun preloadInitialDataIfEmpty() {
        viewModelScope.launch {
            if (repository.getMinutesCount() == 0) {
                // Pre-populate with the user's sample meeting minute
                val sampleResult = parserService.parseLocalMinuteText(AtaParserService.SAMPLE_ATA_TEXT)
                repository.saveParsedAta(sampleResult)

                // Add sample reminders
                repository.insertReminder(
                    ReminderEntity(
                        title = "Consultar andamento da decisão com Paulo e Milton",
                        description = "Verificar contato com os envolvidos e testemunhas para a reunião do corpo.",
                        reminderDateMillis = DateUtils.getStartOfDay() + (1000L * 60 * 60 * 10), // Today 10:00
                        reminderTime = "10:00",
                        category = "Prazo de Ata",
                        isPriority = true
                    )
                )

                repository.insertReminder(
                    ReminderEntity(
                        title = "Reunião de acompanhamento das decisões",
                        description = "Alinhar prazos das atas com Leonardo e Leandro.",
                        reminderDateMillis = DateUtils.addDaysToCurrentTime(2),
                        reminderTime = "19:30",
                        category = "Reunião",
                        isPriority = false
                    )
                )
            }
        }
    }
}
