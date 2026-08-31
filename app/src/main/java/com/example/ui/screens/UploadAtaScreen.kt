package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ExtractedAtaResult
import com.example.data.model.ExtractedDecisionItem
import com.example.service.AtaParserService
import com.example.ui.components.DecisionTypeBadge
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate700
import com.example.ui.theme.TealAccent
import com.example.ui.viewmodel.MainViewModel
import com.example.util.DateUtils

@Composable
fun UploadAtaScreen(
    viewModel: MainViewModel,
    onSavedSuccessfully: () -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    val isProcessing by viewModel.isProcessingAta.collectAsStateWithLifecycle()
    val extractedResult by viewModel.lastExtractedAta.collectAsStateWithLifecycle()

    // Local state for modifying deadline days on preview before saving
    var previewResult by remember(extractedResult) { mutableStateOf(extractedResult) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Receber & Interpretar Ata",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = NavyDark
                )
                Text(
                    text = "Cole o texto da ata ou carregue um modelo para o sistema extrair prazos e responsáveis.",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B)
                )
            }
        }

        // Action bar for sample templates
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        inputText = AtaParserService.SAMPLE_ATA_TEXT
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = NavyPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Exemplo (26/08/2026)", fontSize = 12.sp)
                }

                if (inputText.isNotBlank()) {
                    IconButton(
                        onClick = {
                            inputText = ""
                            viewModel.clearExtractedAta()
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Limpar texto")
                    }
                }
            }
        }

        // Text input field
        item {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                label = { Text("Texto da Ata de Reunião") },
                placeholder = {
                    Text("Cole aqui a ata com assuntos, decisões, responsáveis e prazos...")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                shape = RoundedCornerShape(12.dp),
                maxLines = 15
            )
        }

        // Process button
        item {
            Button(
                onClick = {
                    viewModel.parseAndProcessAta(inputText)
                },
                enabled = inputText.isNotBlank() && !isProcessing,
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Interpretando Ata com IA...", fontSize = 14.sp)
                } else {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Interpretar Ata & Extrair Prazos", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // EXTRACTED PREVIEW SECTION
        previewResult?.let { result ->
            item {
                Divider(modifier = Modifier.padding(vertical = 4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = TealAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Decisões e Prazos Interpretados (${result.decisions.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = NavyDark
                    )
                }
            }

            // Meeting info summary card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate100)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Data: ${result.meetingDateStr}",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = NavyPrimary
                            )
                        }

                        if (result.topics.isNotEmpty()) {
                            Text(
                                text = "Assunto Principal: ${result.topics.joinToString(" • ")}",
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                color = Slate700
                            )
                        }

                        if (result.extraTopics.isNotBlank()) {
                            Text(
                                text = "Pauta Extra: ${result.extraTopics}",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }

                        if (result.attendees.isNotEmpty()) {
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(imageVector = Icons.Default.People, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Presentes: ${result.attendees.joinToString(", ")}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }
            }

            // Extracted decisions items with deadline customization
            itemsIndexed(result.decisions) { index, item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    color = NavyPrimary.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "Decisão #${index + 1}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = NavyPrimary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                DecisionTypeBadge(
                                    decisionType = item.decisionType,
                                    typeLabel = item.typeLabel
                                )
                            }

                            Text(
                                text = if (item.decisionType == "PERMANENT_PROCEDURE") "Permanente" else "Prazo: ${item.deadlineDays} dias",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = TealAccent
                            )
                        }

                        Text(
                            text = item.description,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 20.sp
                        )

                        // Assignees
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = TealAccent, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Designados: ${item.assignees.joinToString(", ")}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TealAccent
                            )
                        }

                        // Fast Deadline selector chips
                        Text(
                            text = "Ajustar prazo / tipo:",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                Pair(7, "7 dias"),
                                Pair(15, "15 dias (Padrão)"),
                                Pair(30, "30 dias"),
                                Pair(0, "Permanente")
                            ).forEach { (days, label) ->
                                val isSelected = if (days == 0) item.decisionType == "PERMANENT_PROCEDURE" else (item.deadlineDays == days && item.decisionType != "PERMANENT_PROCEDURE")
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        val updatedDecisions = result.decisions.toMutableList()
                                        if (days == 0) {
                                            updatedDecisions[index] = item.copy(
                                                deadlineDays = 0,
                                                deadlineDateStr = "Permanente / Contínuo",
                                                decisionType = "PERMANENT_PROCEDURE",
                                                typeLabel = "Decisão permanente de procedimento"
                                            )
                                        } else {
                                            val isFollowUp = days == 15
                                            updatedDecisions[index] = item.copy(
                                                deadlineDays = days,
                                                deadlineDateStr = if (isFollowUp) "Não definido (15 dias padrão)" else DateUtils.formatMillisToDate(DateUtils.addDaysToCurrentTime(days)),
                                                decisionType = if (isFollowUp) "FOLLOW_UP_ASSIGNMENT" else "ACTION_DEADLINE",
                                                typeLabel = if (isFollowUp) "Atribuição de acompanhamento" else "Ação com prazo"
                                            )
                                        }
                                        previewResult = result.copy(decisions = updatedDecisions)
                                    },
                                    label = { Text(label, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }
            }

            // Save confirmation button
            item {
                Button(
                    onClick = {
                        previewResult?.let { toSave ->
                            viewModel.saveExtractedAta(toSave) {
                                inputText = ""
                                onSavedSuccessfully()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Salvar no Sistema & Ativar Alertas de Prazo",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
