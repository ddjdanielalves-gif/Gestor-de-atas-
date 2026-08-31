package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DecisionEntity
import com.example.data.model.DecisionStatus
import com.example.data.model.PriorityLevel
import com.example.ui.theme.NavyPrimary
import com.example.util.DateUtils

@Composable
fun EditDecisionDialog(
    decision: DecisionEntity,
    onDismiss: () -> Unit,
    onSave: (DecisionEntity) -> Unit
) {
    var description by remember { mutableStateOf(decision.description) }
    var assignees by remember { mutableStateOf(decision.assignees) }
    var topicTitle by remember { mutableStateOf(decision.topicTitle) }
    var daysExtension by remember { mutableStateOf(0) }
    var selectedPriority by remember { mutableStateOf(decision.priority) }
    var selectedStatus by remember { mutableStateOf(decision.status) }

    val currentDueDate = if (daysExtension != 0) {
        decision.dueDateMillis + (daysExtension * 24L * 60 * 60 * 1000)
    } else {
        decision.dueDateMillis
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Editar Decisão e Prazo",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrição da Ação / Decisão") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )

                OutlinedTextField(
                    value = assignees,
                    onValueChange = { assignees = it },
                    label = { Text("Responsáveis (separados por vírgula)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = topicTitle,
                    onValueChange = { topicTitle = it },
                    label = { Text("Pauta / Assunto Relacionado") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Ajustar Prazo de Conclusão:",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                Text(
                    text = "Data atual: ${DateUtils.formatMillisToDate(currentDueDate)}",
                    fontSize = 12.sp,
                    color = NavyPrimary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = daysExtension == 0,
                        onClick = { daysExtension = 0 },
                        label = { Text("Manter") }
                    )
                    FilterChip(
                        selected = daysExtension == 7,
                        onClick = { daysExtension = 7 },
                        label = { Text("+7 dias") }
                    )
                    FilterChip(
                        selected = daysExtension == 15,
                        onClick = { daysExtension = 15 },
                        label = { Text("+15 dias") }
                    )
                    FilterChip(
                        selected = daysExtension == 30,
                        onClick = { daysExtension = 30 },
                        label = { Text("+30 dias") }
                    )
                }

                Text(
                    text = "Status:",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedStatus == DecisionStatus.PENDING,
                        onClick = { selectedStatus = DecisionStatus.PENDING },
                        label = { Text("Pendente") }
                    )
                    FilterChip(
                        selected = selectedStatus == DecisionStatus.IN_PROGRESS,
                        onClick = { selectedStatus = DecisionStatus.IN_PROGRESS },
                        label = { Text("Em Andamento") }
                    )
                    FilterChip(
                        selected = selectedStatus == DecisionStatus.COMPLETED,
                        onClick = { selectedStatus = DecisionStatus.COMPLETED },
                        label = { Text("Concluída") }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updated = decision.copy(
                        description = description,
                        assignees = assignees,
                        topicTitle = topicTitle,
                        dueDateMillis = currentDueDate,
                        status = selectedStatus,
                        priority = selectedPriority,
                        completedAt = if (selectedStatus == DecisionStatus.COMPLETED) System.currentTimeMillis() else null
                    )
                    onSave(updated)
                },
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
            ) {
                Text("Salvar Alterações")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
