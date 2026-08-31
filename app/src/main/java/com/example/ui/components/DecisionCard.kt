package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DecisionEntity
import com.example.data.model.DecisionStatus
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.OnTrackGreen
import com.example.ui.theme.OverdueRed
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate700
import com.example.ui.theme.TealAccent
import com.example.util.DateUtils
import com.example.util.DeadlineUrgency

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DecisionCard(
    decision: DecisionEntity,
    onStatusChange: (DecisionStatus) -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onAssigneeClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val isCompleted = decision.status == DecisionStatus.COMPLETED
    val urgency = DateUtils.getUrgency(decision.dueDateMillis, isCompleted)

    val cardBorder = when (urgency) {
        DeadlineUrgency.OVERDUE -> BorderStroke(1.5.dp, OverdueRed.copy(alpha = 0.5f))
        DeadlineUrgency.TODAY -> BorderStroke(1.5.dp, Color(0xFFD97706).copy(alpha = 0.5f))
        else -> BorderStroke(1.dp, Slate200)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) Slate100.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surface
        ),
        border = cardBorder,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCompleted) 0.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header: Topic, Type and Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (decision.topicTitle.isNotBlank()) {
                        Surface(
                            color = NavyPrimary.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = decision.topicTitle,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NavyPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    DecisionTypeBadge(
                        decisionType = decision.decisionType,
                        typeLabel = decision.typeLabel
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (decision.decisionType == "PERMANENT_PROCEDURE") {
                        Surface(
                            color = Color(0xFFEDE9FE),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Regra Permanente",
                                color = Color(0xFF6D28D9),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    } else {
                        DeadlineStatusBadge(
                            dueDateMillis = decision.dueDateMillis,
                            isCompleted = isCompleted
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Opções",
                                tint = Slate700,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(if (isCompleted) "Marcar como Pendente" else "Marcar como Concluída")
                                },
                                onClick = {
                                    menuExpanded = false
                                    onStatusChange(
                                        if (isCompleted) DecisionStatus.PENDING else DecisionStatus.COMPLETED
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (isCompleted) Icons.Default.RadioButtonUnchecked else Icons.Default.CheckCircle,
                                        contentDescription = null
                                    )
                                }
                            )

                            if (!isCompleted) {
                                DropdownMenuItem(
                                    text = {
                                        Text(if (decision.status == DecisionStatus.IN_PROGRESS) "Marcar como Pendente" else "Marcar Em Andamento")
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onStatusChange(
                                            if (decision.status == DecisionStatus.IN_PROGRESS) DecisionStatus.PENDING else DecisionStatus.IN_PROGRESS
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = null
                                        )
                                    }
                                )
                            }

                            DropdownMenuItem(
                                text = { Text("Editar Prazo e Dados") },
                                onClick = {
                                    menuExpanded = false
                                    onEditClick()
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null
                                    )
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Excluir Decisão", color = OverdueRed) },
                                onClick = {
                                    menuExpanded = false
                                    onDeleteClick()
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = null,
                                        tint = OverdueRed
                                    )
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main description
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                IconButton(
                    onClick = {
                        onStatusChange(
                            if (isCompleted) DecisionStatus.PENDING else DecisionStatus.COMPLETED
                        )
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = if (isCompleted) "Concluído" else "Marcar como feito",
                        tint = if (isCompleted) OnTrackGreen else Slate700,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = decision.description,
                    fontSize = 14.sp,
                    fontWeight = if (isCompleted) FontWeight.Normal else FontWeight.Medium,
                    color = if (isCompleted) Color(0xFF64748B) else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    lineHeight = 20.sp,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Responsáveis (Assignees chips)
            val assigneesList = decision.assignees.split(",").map { it.trim() }.filter { it.isNotBlank() }
            if (assigneesList.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = TealAccent,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Responsáveis: ",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate700
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        assigneesList.forEach { name ->
                            Surface(
                                color = TealAccent.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = name,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TealAccent,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Deadline info footer
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (decision.decisionType == "PERMANENT_PROCEDURE") {
                        "Procedimento permanente (sem prazo de expiração)"
                    } else if (decision.deadlineDescription.isNotBlank()) {
                        decision.deadlineDescription
                    } else {
                        "Data limite: ${DateUtils.formatMillisToReadable(decision.dueDateMillis)}"
                    },
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}
