package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DueTodayAmber
import com.example.ui.theme.NavyPrimary
import com.example.util.DateUtils

@Composable
fun NewReminderDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, description: String, dateMillis: Long, time: String, category: String, isPriority: Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var daysAhead by remember { mutableStateOf(0) } // 0 = Hoje, 1 = Amanhã, 3 = Em 3 dias, 7 = Em 1 semana
    var timeStr by remember { mutableStateOf("09:00") }
    var category by remember { mutableStateOf("Prazo de Ata") }
    var isPriority by remember { mutableStateOf(false) }

    val categories = listOf("Prazo de Ata", "Reunião", "Acompanhamento", "Geral")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Novo Lembrete de Agenda",
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
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título do Lembrete") },
                    placeholder = { Text("Ex: Falar com Leonardo sobre o relatório") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Detalhes adicionais (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                Text(
                    text = "Data do Lembrete:",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = daysAhead == 0,
                        onClick = { daysAhead = 0 },
                        label = { Text("Hoje") }
                    )
                    FilterChip(
                        selected = daysAhead == 1,
                        onClick = { daysAhead = 1 },
                        label = { Text("Amanhã") }
                    )
                    FilterChip(
                        selected = daysAhead == 3,
                        onClick = { daysAhead = 3 },
                        label = { Text("+3 dias") }
                    )
                    FilterChip(
                        selected = daysAhead == 7,
                        onClick = { daysAhead = 7 },
                        label = { Text("+7 dias") }
                    )
                }

                OutlinedTextField(
                    value = timeStr,
                    onValueChange = { timeStr = it },
                    label = { Text("Horário (HH:mm)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Categoria:",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 11.sp) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = isPriority,
                        onClick = { isPriority = !isPriority },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = if (isPriority) DueTodayAmber else NavyPrimary
                            )
                        },
                        label = { Text("Marcar como Alta Prioridade") }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val targetDateMillis = DateUtils.addDaysToCurrentTime(daysAhead)
                        onSave(title, description, targetDateMillis, timeStr, category, isPriority)
                    }
                },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
            ) {
                Text("Adicionar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
