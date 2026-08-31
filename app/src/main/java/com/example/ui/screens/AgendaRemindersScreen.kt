package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.NewReminderDialog
import com.example.ui.components.ReminderCard
import com.example.ui.theme.DueTodayAmber
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate700
import com.example.ui.viewmodel.MainViewModel
import com.example.util.DateUtils

@Composable
fun AgendaRemindersScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val reminders by viewModel.allReminders.collectAsStateWithLifecycle()
    var showNewReminderDialog by remember { mutableStateOf(false) }

    val todayReminders = reminders.filter {
        !it.isCompleted && DateUtils.calculateDaysRemaining(it.reminderDateMillis) == 0L
    }

    val upcomingReminders = reminders.filter {
        !it.isCompleted && DateUtils.calculateDaysRemaining(it.reminderDateMillis) > 0L
    }

    val pastOrCompletedReminders = reminders.filter {
        it.isCompleted || DateUtils.calculateDaysRemaining(it.reminderDateMillis) < 0L
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
                        text = "Agenda de Lembretes",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = NavyDark
                    )
                    Text(
                        text = "Avisos e compromissos sincronizados com as atas",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }

                Button(
                    onClick = { showNewReminderDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Novo Lembrete", fontSize = 12.sp)
                }
            }
        }

        // Active Today Consultation Box
        if (todayReminders.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DueTodayAmber.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = DueTodayAmber,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Você tem ${todayReminders.size} lembrete(s) para hoje!",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = DueTodayAmber
                            )
                            Text(
                                text = "Avisos automáticos ativos durante a consulta do app.",
                                fontSize = 11.sp,
                                color = Slate700
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Lembretes de Hoje",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Slate700
                )
            }

            items(todayReminders, key = { it.id }) { reminder ->
                ReminderCard(
                    reminder = reminder,
                    onToggleCompleted = { isDone -> viewModel.toggleReminderCompleted(reminder.id, isDone) },
                    onDelete = { viewModel.deleteReminder(reminder.id) }
                )
            }
        }

        // Upcoming Reminders Section
        if (upcomingReminders.isNotEmpty()) {
            item {
                Text(
                    text = "Próximos Lembretes",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Slate700
                )
            }

            items(upcomingReminders, key = { it.id }) { reminder ->
                ReminderCard(
                    reminder = reminder,
                    onToggleCompleted = { isDone -> viewModel.toggleReminderCompleted(reminder.id, isDone) },
                    onDelete = { viewModel.deleteReminder(reminder.id) }
                )
            }
        }

        // Completed or past
        if (pastOrCompletedReminders.isNotEmpty()) {
            item {
                Text(
                    text = "Concluídos & Histórico",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF64748B)
                )
            }

            items(pastOrCompletedReminders, key = { it.id }) { reminder ->
                ReminderCard(
                    reminder = reminder,
                    onToggleCompleted = { isDone -> viewModel.toggleReminderCompleted(reminder.id, isDone) },
                    onDelete = { viewModel.deleteReminder(reminder.id) }
                )
            }
        }

        if (reminders.isEmpty()) {
            item {
                Surface(
                    color = Slate100,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = NavyPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = "Nenhum lembrete na agenda",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Slate700
                        )
                        Text(
                            text = "Toque em 'Novo Lembrete' para registrar compromissos e prazos.",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }
        }
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
