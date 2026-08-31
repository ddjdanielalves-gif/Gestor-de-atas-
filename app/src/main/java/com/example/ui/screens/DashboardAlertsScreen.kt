package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DecisionEntity
import com.example.data.model.DecisionStatus
import com.example.ui.components.AlertSummaryBanner
import com.example.ui.components.DecisionCard
import com.example.ui.components.EditDecisionDialog
import com.example.ui.components.NewReminderDialog
import com.example.ui.components.ReminderCard
import com.example.ui.theme.DueSoonOrange
import com.example.ui.theme.DueTodayAmber
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.OnTrackGreen
import com.example.ui.theme.OverdueRed
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate700
import com.example.ui.viewmodel.MainViewModel
import com.example.util.DateUtils
import com.example.util.DeadlineUrgency

@Composable
fun DashboardAlertsScreen(
    viewModel: MainViewModel,
    onNavigateToUpload: () -> Unit,
    onNavigateToDecisions: () -> Unit,
    onNavigateToAgenda: () -> Unit,
    modifier: Modifier = Modifier
) {
    val decisions by viewModel.allDecisions.collectAsStateWithLifecycle()
    val reminders by viewModel.allReminders.collectAsStateWithLifecycle()
    val alertsSummary by viewModel.alertsSummary.collectAsStateWithLifecycle()
    val notice by viewModel.userConsultationNotice.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedAssignee by viewModel.selectedAssigneeFilter.collectAsStateWithLifecycle()

    var editingDecision by remember { mutableStateOf<DecisionEntity?>(null) }
    var showNewReminderDialog by remember { mutableStateOf(false) }

    // Filtered decisions for dashboard view
    val filteredDecisions = remember(decisions, searchQuery, selectedAssignee) {
        decisions.filter { dec ->
            val matchesQuery = searchQuery.isBlank() ||
                    dec.description.contains(searchQuery, ignoreCase = true) ||
                    dec.assignees.contains(searchQuery, ignoreCase = true) ||
                    dec.topicTitle.contains(searchQuery, ignoreCase = true)

            val matchesAssignee = selectedAssignee == null ||
                    dec.assignees.contains(selectedAssignee!!, ignoreCase = true)

            matchesQuery && matchesAssignee
        }
    }

    val overdueList = filteredDecisions.filter {
        it.status != DecisionStatus.COMPLETED &&
                DateUtils.getUrgency(it.dueDateMillis, false) == DeadlineUrgency.OVERDUE
    }

    val dueTodayList = filteredDecisions.filter {
        it.status != DecisionStatus.COMPLETED &&
                DateUtils.getUrgency(it.dueDateMillis, false) == DeadlineUrgency.TODAY
    }

    val dueSoonList = filteredDecisions.filter {
        it.status != DecisionStatus.COMPLETED &&
                DateUtils.getUrgency(it.dueDateMillis, false) == DeadlineUrgency.SOON
    }

    val activeRemindersToday = reminders.filter {
        !it.isCompleted && DateUtils.calculateDaysRemaining(it.reminderDateMillis) == 0L
    }

    // List of unique assignees for quick filter pills
    val allAssignees = remember(decisions) {
        decisions.flatMap { it.assignees.split(",") }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Notification pill if present
        if (notice != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = notice!!,
                            color = Color(0xFF166534),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.dismissNotice() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Fechar",
                                tint = Color(0xFF166534),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Top Alert & Consultation Summary Banner
        item {
            AlertSummaryBanner(
                summary = alertsSummary,
                onFilterClick = { filterType ->
                    when (filterType) {
                        "LEMBRETES" -> onNavigateToAgenda()
                        else -> {
                            viewModel.setStatusFilter(filterType)
                            onNavigateToDecisions()
                        }
                    }
                }
            )
        }

        // Search and Assignee Filter bar
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Consultar decisões, responsáveis ou pautas...") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Limpar")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                if (allAssignees.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedAssignee == null,
                                onClick = { viewModel.setAssigneeFilter(null) },
                                label = { Text("Todos Responsáveis") }
                            )
                        }
                        items(allAssignees) { name ->
                            FilterChip(
                                selected = selectedAssignee == name,
                                onClick = {
                                    viewModel.setAssigneeFilter(if (selectedAssignee == name) null else name)
                                },
                                label = { Text(name) }
                            )
                        }
                    }
                }
            }
        }

        // OVERDUE SECTION (ATRASADAS)
        if (overdueList.isNotEmpty()) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = OverdueRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Decisões com Prazo Vencido (${overdueList.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = OverdueRed
                    )
                }
            }

            items(overdueList, key = { "overdue_${it.id}" }) { decision ->
                DecisionCard(
                    decision = decision,
                    onStatusChange = { newStatus -> viewModel.updateDecisionStatus(decision.id, newStatus) },
                    onEditClick = { editingDecision = decision },
                    onDeleteClick = { viewModel.deleteDecision(decision.id) },
                    onAssigneeClick = { name -> viewModel.setAssigneeFilter(name) }
                )
            }
        }

        // DUE TODAY SECTION (VENCE HOJE)
        if (dueTodayList.isNotEmpty()) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = DueTodayAmber,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Vencendo Hoje (${dueTodayList.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = DueTodayAmber
                    )
                }
            }

            items(dueTodayList, key = { "today_${it.id}" }) { decision ->
                DecisionCard(
                    decision = decision,
                    onStatusChange = { newStatus -> viewModel.updateDecisionStatus(decision.id, newStatus) },
                    onEditClick = { editingDecision = decision },
                    onDeleteClick = { viewModel.deleteDecision(decision.id) },
                    onAssigneeClick = { name -> viewModel.setAssigneeFilter(name) }
                )
            }
        }

        // REMINDERS TODAY SECTION
        if (activeRemindersToday.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Lembretes da Agenda para Hoje (${activeRemindersToday.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF0284C7)
                        )
                    }

                    Button(
                        onClick = { showNewReminderDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Lembrete", fontSize = 12.sp)
                    }
                }
            }

            items(activeRemindersToday, key = { "rem_${it.id}" }) { reminder ->
                ReminderCard(
                    reminder = reminder,
                    onToggleCompleted = { isDone -> viewModel.toggleReminderCompleted(reminder.id, isDone) },
                    onDelete = { viewModel.deleteReminder(reminder.id) }
                )
            }
        }

        // DUE SOON SECTION (PRÓXIMOS 7 DIAS)
        if (dueSoonList.isNotEmpty()) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = DueSoonOrange,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Vencendo nos Próximos Dias (${dueSoonList.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Slate700
                    )
                }
            }

            items(dueSoonList, key = { "soon_${it.id}" }) { decision ->
                DecisionCard(
                    decision = decision,
                    onStatusChange = { newStatus -> viewModel.updateDecisionStatus(decision.id, newStatus) },
                    onEditClick = { editingDecision = decision },
                    onDeleteClick = { viewModel.deleteDecision(decision.id) },
                    onAssigneeClick = { name -> viewModel.setAssigneeFilter(name) }
                )
            }
        }

        // Empty state if no alerts
        if (overdueList.isEmpty() && dueTodayList.isEmpty() && dueSoonList.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate100),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFDCFCE7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = OnTrackGreen,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Nenhuma decisão pendente de atenção imediata",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Slate700
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Faça upload de uma nova ata ou consulte a lista geral de deliberações.",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = onNavigateToUpload,
                                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                            ) {
                                Text("Receber Nova Ata")
                            }
                            Button(
                                onClick = onNavigateToDecisions,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569))
                            ) {
                                Text("Ver Todas")
                            }
                        }
                    }
                }
            }
        }
    }

    editingDecision?.let { dec ->
        EditDecisionDialog(
            decision = dec,
            onDismiss = { editingDecision = null },
            onSave = { updated ->
                viewModel.updateDecision(updated)
                editingDecision = null
            }
        )
    }

    if (showNewReminderDialog) {
        NewReminderDialog(
            onDismiss = { showNewReminderDialog = false },
            onSave = { title, desc, date, time, cat, isPrio ->
                viewModel.addReminder(title, desc, date, time, cat, isPrio)
                showNewReminderDialog = false
            }
        )
    }
}
