package com.example.ui.screens

import android.content.Intent
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DecisionEntity
import com.example.data.model.DecisionStatus
import com.example.ui.components.DecisionCard
import com.example.ui.components.EditDecisionDialog
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.Slate700
import com.example.ui.theme.TealAccent
import com.example.ui.viewmodel.MainViewModel
import com.example.util.DateUtils
import com.example.util.DeadlineUrgency

@Composable
fun DecisionsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allDecisions by viewModel.allDecisions.collectAsStateWithLifecycle()
    val statusFilter by viewModel.selectedStatusFilter.collectAsStateWithLifecycle()
    val selectedAssignee by viewModel.selectedAssigneeFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    var editingDecision by remember { mutableStateOf<DecisionEntity?>(null) }

    // List of unique assignees
    val allAssignees = remember(allDecisions) {
        allDecisions.flatMap { it.assignees.split(",") }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    val filteredList = remember(allDecisions, statusFilter, selectedAssignee, searchQuery) {
        allDecisions.filter { dec ->
            val matchesQuery = searchQuery.isBlank() ||
                    dec.description.contains(searchQuery, ignoreCase = true) ||
                    dec.assignees.contains(searchQuery, ignoreCase = true) ||
                    dec.topicTitle.contains(searchQuery, ignoreCase = true)

            val matchesAssignee = selectedAssignee == null ||
                    dec.assignees.contains(selectedAssignee!!, ignoreCase = true)

            val urgency = DateUtils.getUrgency(dec.dueDateMillis, dec.status == DecisionStatus.COMPLETED)
            val matchesStatus = when (statusFilter) {
                "ATRASADAS" -> dec.status != DecisionStatus.COMPLETED && urgency == DeadlineUrgency.OVERDUE
                "HOJE" -> dec.status != DecisionStatus.COMPLETED && urgency == DeadlineUrgency.TODAY
                "EM_BREVE" -> dec.status != DecisionStatus.COMPLETED && urgency == DeadlineUrgency.SOON
                "PENDENTES" -> dec.status != DecisionStatus.COMPLETED
                "CONCLUIDAS" -> dec.status == DecisionStatus.COMPLETED
                else -> true // "TODAS"
            }

            matchesQuery && matchesAssignee && matchesStatus
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Decisões & Prazos",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = NavyDark
                    )
                    Text(
                        text = "${filteredList.size} de ${allDecisions.size} decisões exibidas",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }

                OutlinedButton(
                    onClick = {
                        val report = StringBuilder()
                        report.append("📋 *RELATÓRIO DE DECISÕES E PRAZOS*\n\n")
                        filteredList.forEachIndexed { i, d ->
                            val statusStr = if (d.status == DecisionStatus.COMPLETED) "✅ Concluída" else "⏰ ${DateUtils.getDeadlineBadgeText(d.dueDateMillis, false)}"
                            report.append("${i + 1}. *${d.description}*\n")
                            report.append("   👤 Responsáveis: ${d.assignees}\n")
                            report.append("   📅 Prazo: ${DateUtils.formatMillisToDate(d.dueDateMillis)} ($statusStr)\n\n")
                        }

                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, report.toString())
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Compartilhar Decisões"))
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Exportar", fontSize = 12.sp)
                }
            }
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Buscar por responsável, texto ou pauta...") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
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
        }

        // Status Filter Pills
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                val filters = listOf(
                    "TODAS" to "Todas",
                    "ATRASADAS" to "⚠️ Vencidas",
                    "HOJE" to "⏰ Vencem Hoje",
                    "EM_BREVE" to "⏳ Próximos Dias",
                    "PENDENTES" to "Em Aberto",
                    "CONCLUIDAS" to "✅ Concluídas"
                )

                items(filters) { (key, label) ->
                    FilterChip(
                        selected = statusFilter == key,
                        onClick = { viewModel.setStatusFilter(key) },
                        label = { Text(label, fontSize = 12.sp) }
                    )
                }
            }
        }

        // Assignee Filter Pills
        if (allAssignees.isNotEmpty()) {
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedAssignee == null,
                            onClick = { viewModel.setAssigneeFilter(null) },
                            label = { Text("Todos Integrantes", fontSize = 11.sp) }
                        )
                    }
                    items(allAssignees) { name ->
                        FilterChip(
                            selected = selectedAssignee == name,
                            onClick = {
                                viewModel.setAssigneeFilter(if (selectedAssignee == name) null else name)
                            },
                            label = { Text(name, fontSize = 11.sp) }
                        )
                    }
                }
            }
        }

        // Items list
        if (filteredList.isEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Nenhuma decisão encontrada para o filtro selecionado",
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = Slate700
                        )
                    }
                }
            }
        } else {
            items(filteredList, key = { it.id }) { decision ->
                DecisionCard(
                    decision = decision,
                    onStatusChange = { newStatus -> viewModel.updateDecisionStatus(decision.id, newStatus) },
                    onEditClick = { editingDecision = decision },
                    onDeleteClick = { viewModel.deleteDecision(decision.id) },
                    onAssigneeClick = { name -> viewModel.setAssigneeFilter(name) }
                )
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
}
