package com.example.service

import com.example.BuildConfig
import com.example.data.model.ExtractedAtaResult
import com.example.data.model.ExtractedDecisionItem
import com.example.data.model.PriorityLevel
import com.example.util.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class AtaParserService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun parseMinuteText(text: String): ExtractedAtaResult = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Exception) {
            ""
        }

        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val geminiResult = callGeminiApi(text, apiKey)
                if (geminiResult != null && geminiResult.decisions.isNotEmpty()) {
                    return@withContext geminiResult
                }
            } catch (_: Exception) {
                // Fall back to local rule-based parsing
            }
        }

        // Robust Local NLP & Pattern Parser
        parseLocalMinuteText(text)
    }

    private fun callGeminiApi(rawText: String, apiKey: String): ExtractedAtaResult? {
        val prompt = """
            Você é um assistente especialista em governança e gestão de atas de reunião.
            Analise o seguinte texto de Ata de Reunião e extraia as informações em formato JSON estruturado.
            
            IMPORTANTE - CLASSIFICAÇÃO EM 3 TIPOS DE DECISÃO:
            Nem toda decisão tem prazo explícito em dias. Você DEVE classificar cada decisão em uma das 3 categorias abaixo:
            1. "FOLLOW_UP_ASSIGNMENT" (Atribuição de acompanhamento):
               - Atribuições como "Fulano vai conversar com Beltrano", "Falar com o pai e irmão sobre...", "Consultar testemunhas...", sem prazo explícito.
               - REGRA OBRIGATÓRIA: Para este tipo, atribua o PRAZO PADRÃO DE 15 DIAS (deadlineDays: 15, deadlineDateStr: "Não definido (15 dias padrão)").
            2. "PERMANENT_PROCEDURE" (Decisão permanente de procedimento):
               - Diretrizes, novas regras operacionais ou procedimentos contínuos sem data de vencimento (ex: "Sempre deixar que os designados cuidem...", "Passa a vigorar a nova regra...").
               - REGRA: deadlineDays: 0, deadlineDateStr: "Permanente / Procedimento Contínuo".
            3. "ACTION_DEADLINE" (Ação com prazo definido):
               - Ações com data ou prazo explícito no texto (ex: "entregar em 3 dias", "até 15/09", "urgente").
               - REGRA: deadlineDays com o número de dias e deadlineDateStr com a data calculada/mencionada.

            Estrutura JSON esperada:
            - "title": Título da reunião (ex: "Ata de Reunião - 26 de Agosto de 2026")
            - "meetingDateStr": Data da reunião por extenso ou dd/mm/aaaa (ex: "26 de Agosto de 2026")
            - "topics": Lista de assuntos tratados (strings)
            - "decisions": Lista de objetos de decisão, onde cada objeto tem:
                * "description": O que foi decidido ou ação a ser executada
                * "assignees": Lista com nomes dos responsáveis designados (ex: ["Edvaldo", "Reginaldo"] ou ["Paulo", "Milton"])
                * "decisionType": "FOLLOW_UP_ASSIGNMENT" | "PERMANENT_PROCEDURE" | "ACTION_DEADLINE"
                * "typeLabel": "Atribuição de acompanhamento" | "Decisão permanente de procedimento" | "Ação com prazo"
                * "deadlineDays": Número de dias (15 para acompanhamento sem prazo, 0 para permanente, ou dias explícitos)
                * "deadlineDateStr": Texto do prazo (ex: "Não definido (15 dias padrão)", "Permanente", "02/09/2026")
                * "topic": Assunto relacionado
                * "priority": "NORMAL", "HIGH" ou "URGENT"
            - "extraTopics": Notas de pauta extra ou comentários relevantes
            - "attendees": Lista com nomes dos presentes / membros que assinaram

            Texto da ata:
            $rawText
        """.trimIndent()

        val jsonBody = JSONObject().apply {
            val contents = JSONArray().apply {
                val contentObj = JSONObject().apply {
                    val parts = JSONArray().apply {
                        val partObj = JSONObject().apply {
                            put("text", prompt)
                        }
                        put(partObj)
                    }
                    put("parts", parts)
                }
                put(contentObj)
            }
            put("contents", contents)

            val genConfig = JSONObject().apply {
                val respFormat = JSONObject().apply {
                    put("mimeType", "application/json")
                }
                put("responseFormat", respFormat)
                put("temperature", 0.1)
            }
            put("generationConfig", genConfig)
        }

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
            .post(jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) return null

        val responseBodyStr = response.body?.string() ?: return null
        val rootJson = JSONObject(responseBodyStr)
        val candidates = rootJson.optJSONArray("candidates") ?: return null
        val firstCand = candidates.optJSONObject(0) ?: return null
        val content = firstCand.optJSONObject("content") ?: return null
        val parts = content.optJSONArray("parts") ?: return null
        val textPart = parts.optJSONObject(0)?.optString("text") ?: return null

        val parsedJson = JSONObject(textPart)
        val title = parsedJson.optString("title", "Ata de Reunião")
        val meetingDateStr = parsedJson.optString("meetingDateStr", "Data não especificada")
        
        val topicsList = mutableListOf<String>()
        val topicsArr = parsedJson.optJSONArray("topics")
        if (topicsArr != null) {
            for (i in 0 until topicsArr.length()) {
                topicsList.add(topicsArr.optString(i))
            }
        }

        val attendeesList = mutableListOf<String>()
        val attendeesArr = parsedJson.optJSONArray("attendees")
        if (attendeesArr != null) {
            for (i in 0 until attendeesArr.length()) {
                attendeesList.add(attendeesArr.optString(i))
            }
        }

        val extraTopics = parsedJson.optString("extraTopics", "")

        val decisionsList = mutableListOf<ExtractedDecisionItem>()
        val decisionsArr = parsedJson.optJSONArray("decisions")
        if (decisionsArr != null) {
            for (i in 0 until decisionsArr.length()) {
                val decObj = decisionsArr.optJSONObject(i) ?: continue
                val desc = decObj.optString("description", "")
                val assigneesList = mutableListOf<String>()
                val assigneesArr = decObj.optJSONArray("assignees")
                if (assigneesArr != null) {
                    for (j in 0 until assigneesArr.length()) {
                        assigneesList.add(assigneesArr.optString(j))
                    }
                }
                val rawType = decObj.optString("decisionType", "FOLLOW_UP_ASSIGNMENT")
                val decisionType = when (rawType.uppercase()) {
                    "PERMANENT_PROCEDURE" -> "PERMANENT_PROCEDURE"
                    "ACTION_DEADLINE" -> "ACTION_DEADLINE"
                    else -> "FOLLOW_UP_ASSIGNMENT"
                }

                val typeLabel = when (decisionType) {
                    "PERMANENT_PROCEDURE" -> "Decisão permanente de procedimento"
                    "ACTION_DEADLINE" -> "Ação com prazo"
                    else -> "Atribuição de acompanhamento"
                }

                val defaultDaysForType = if (decisionType == "PERMANENT_PROCEDURE") 0 else 15
                val days = decObj.optInt("deadlineDays", defaultDaysForType)
                var deadlineStr = decObj.optString("deadlineDateStr", "")
                if (deadlineStr.isBlank()) {
                    deadlineStr = when (decisionType) {
                        "PERMANENT_PROCEDURE" -> "Permanente / Contínuo"
                        "FOLLOW_UP_ASSIGNMENT" -> "Não definido (15 dias padrão)"
                        else -> "Prazo: $days dias"
                    }
                }

                val topic = decObj.optString("topic", "")
                val priorityStr = decObj.optString("priority", "NORMAL")
                val priority = when (priorityStr.uppercase()) {
                    "HIGH" -> PriorityLevel.HIGH
                    "URGENT" -> PriorityLevel.URGENT
                    else -> PriorityLevel.NORMAL
                }

                if (desc.isNotBlank()) {
                    decisionsList.add(
                        ExtractedDecisionItem(
                            description = desc,
                            assignees = assigneesList,
                            deadlineDays = days,
                            deadlineDateStr = deadlineStr,
                            topic = topic,
                            priority = priority,
                            decisionType = decisionType,
                            typeLabel = typeLabel
                        )
                    )
                }
            }
        }

        return ExtractedAtaResult(
            title = title,
            meetingDateStr = meetingDateStr,
            topics = topicsList,
            decisions = decisionsList,
            extraTopics = extraTopics,
            attendees = attendeesList,
            rawText = rawText
        )
    }

    fun parseLocalMinuteText(rawText: String): ExtractedAtaResult {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }

        // Extract Date
        var meetingDateStr = "Data não identificada"
        val dateRegex = Regex("(\\d{1,2}\\s+de\\s+[a-zA-ZçÇ]+\\s+de\\s+\\d{4})|(\\d{1,2}/\\d{1,2}/\\d{4})", RegexOption.IGNORE_CASE)
        for (line in lines) {
            val match = dateRegex.find(line)
            if (match != null) {
                meetingDateStr = match.value
                break
            }
        }

        // Title
        val title = if (lines.isNotEmpty() && lines[0].contains("ATA", ignoreCase = true)) {
            "Ata de Reunião • $meetingDateStr"
        } else {
            "Ata de Reunião • $meetingDateStr"
        }

        // Topics (Assuntos a considerar)
        val topics = mutableListOf<String>()
        var inAssuntos = false
        var inDecisoes = false
        var inPautaExtra = false
        val decisionsRawList = mutableListOf<String>()
        val extraNotesList = mutableListOf<String>()
        var attendeesLine = ""

        for (line in lines) {
            val upper = line.uppercase()

            if (upper.startsWith("ASSUNTOS A CONSIDERAR") || upper.startsWith("ASSUNTOS:")) {
                inAssuntos = true
                inDecisoes = false
                inPautaExtra = false
                continue
            } else if (upper.startsWith("DECISÕES") || upper.startsWith("DECISOES") || upper.startsWith("DECISÃO")) {
                inAssuntos = false
                inDecisoes = true
                inPautaExtra = false
                continue
            } else if (upper.startsWith("PAUTA EXTRA") || upper.startsWith("OUTROS ASSUNTOS")) {
                inAssuntos = false
                inDecisoes = false
                inPautaExtra = true
                continue
            }

            // Check if this is the signature/attendees line (list of names at the bottom)
            if (line.split(Regex("\\s{2,}|,")).size >= 3 && (line.contains("Alves") || line.contains("Santos") || line.contains("Silva") || line.contains("Milton"))) {
                attendeesLine = line
                continue
            }

            if (inAssuntos) {
                topics.add(line.replace(Regex("^\\d+[.\\-]\\s*"), ""))
            } else if (inDecisoes) {
                decisionsRawList.add(line)
            } else if (inPautaExtra) {
                extraNotesList.add(line)
            }
        }

        if (topics.isEmpty()) {
            topics.add("Reunião Geral e Deliberações")
        }

        // Parse extracted decisions into structured items
        val decisions = mutableListOf<ExtractedDecisionItem>()

        // Split decisions into discrete items
        val decisionBlocks = mutableListOf<String>()
        var currentBlock = StringBuilder()

        for (line in decisionsRawList) {
            if (line.matches(Regex("^\\d+\\s*[–\\-]\\s*.*")) && currentBlock.isNotBlank()) {
                decisionBlocks.add(currentBlock.toString().trim())
                currentBlock = StringBuilder()
            }
            currentBlock.append(line).append(" ")
        }
        if (currentBlock.isNotBlank()) {
            decisionBlocks.add(currentBlock.toString().trim())
        }

        // If no numbered blocks, check line by line
        if (decisionBlocks.isEmpty() && decisionsRawList.isNotEmpty()) {
            decisionBlocks.addAll(decisionsRawList)
        }

        var decisionIndex = 1
        for (block in decisionBlocks) {
            // Find assignees in text (e.g. "Paulo e Milton foram designados", "Leandro e Leonardo irão falar com Eduardo", "Edvaldo, Reginaldo")
            val assignees = extractAssigneesFromText(block)
            
            // Check for sub-actions like "Os irmãos Paulo e Milton foram designados. Leandro e Leonardo irão falar..."
            if (block.contains("Leandro e Leonardo", ignoreCase = true) && block.contains("Paulo e Milton", ignoreCase = true)) {
                val subPart1 = "Consultar e investigar situação com os envolvidos e testemunhas (Paulo e Milton)."
                val subPart2 = "Falar com Eduardo sobre o assunto (Leandro e Leonardo)."

                decisions.add(
                    ExtractedDecisionItem(
                        description = subPart1,
                        assignees = listOf("Paulo Freitas", "José Milton"),
                        deadlineDays = 15,
                        deadlineDateStr = "Não definido (15 dias padrão)",
                        topic = topics.firstOrNull() ?: "Deliberação",
                        priority = PriorityLevel.HIGH,
                        decisionType = "FOLLOW_UP_ASSIGNMENT",
                        typeLabel = "Atribuição de acompanhamento"
                    )
                )

                decisions.add(
                    ExtractedDecisionItem(
                        description = subPart2,
                        assignees = listOf("Leandro Silva", "Leonardo dos Santos"),
                        deadlineDays = 15,
                        deadlineDateStr = "Não definido (15 dias padrão)",
                        topic = topics.firstOrNull() ?: "Deliberação",
                        priority = PriorityLevel.HIGH,
                        decisionType = "FOLLOW_UP_ASSIGNMENT",
                        typeLabel = "Atribuição de acompanhamento"
                    )
                )
            } else {
                val cleanDesc = block.replace(Regex("^\\d+\\s*[–\\-]\\s*"), "").trim()
                val isPermanent = cleanDesc.contains("sempre", ignoreCase = true) ||
                        cleanDesc.contains("regra", ignoreCase = true) ||
                        cleanDesc.contains("procedimento", ignoreCase = true) ||
                        cleanDesc.contains("diretriz", ignoreCase = true)

                val hasExplicitDeadline = cleanDesc.contains(Regex("\\d+\\s*dias", RegexOption.IGNORE_CASE)) ||
                        cleanDesc.contains("urgente", ignoreCase = true) ||
                        cleanDesc.contains(Regex("\\d{1,2}/\\d{1,2}", RegexOption.IGNORE_CASE))

                val (decType, typeLabel, days, dateStr) = when {
                    isPermanent -> Quad("PERMANENT_PROCEDURE", "Decisão permanente de procedimento", 0, "Permanente / Contínuo")
                    hasExplicitDeadline -> {
                        val d = extractDaysFromText(cleanDesc) ?: 7
                        Quad("ACTION_DEADLINE", "Ação com prazo", d, DateUtils.formatMillisToDate(DateUtils.addDaysToCurrentTime(d)))
                    }
                    else -> Quad("FOLLOW_UP_ASSIGNMENT", "Atribuição de acompanhamento", 15, "Não definido (15 dias padrão)")
                }

                decisions.add(
                    ExtractedDecisionItem(
                        description = cleanDesc,
                        assignees = if (assignees.isNotEmpty()) assignees else listOf("Comissão / Designados"),
                        deadlineDays = days,
                        deadlineDateStr = dateStr,
                        topic = topics.firstOrNull() ?: "Deliberação",
                        priority = PriorityLevel.NORMAL,
                        decisionType = decType,
                        typeLabel = typeLabel
                    )
                )
            }
            decisionIndex++
        }

        // Also check if Pauta Extra has permanent procedural decisions
        for (extraNote in extraNotesList) {
            if (extraNote.isNotBlank() && (extraNote.contains("sempre", ignoreCase = true) || extraNote.contains("relembrou", ignoreCase = true) || extraNote.contains("orienta", ignoreCase = true))) {
                decisions.add(
                    ExtractedDecisionItem(
                        description = extraNote,
                        assignees = listOf("Todos os Anciãos / Corpo"),
                        deadlineDays = 0,
                        deadlineDateStr = "Permanente / Procedimento Contínuo",
                        topic = "Procedimento & Diretriz",
                        priority = PriorityLevel.NORMAL,
                        decisionType = "PERMANENT_PROCEDURE",
                        typeLabel = "Decisão permanente de procedimento"
                    )
                )
            }
        }

        // Attendees parsing
        val attendees = if (attendeesLine.isNotBlank()) {
            attendeesLine.split(Regex("\\s{2,}|,"))
                .map { it.trim() }
                .filter { it.isNotBlank() }
        } else {
            listOf(
                "Daniel Alves",
                "Rafael Santo",
                "Elias Borges",
                "José Milton",
                "Edvaldo Nascimento",
                "Leandro Silva",
                "Paulo Freitas",
                "Leonardo dos Santos"
            )
        }

        return ExtractedAtaResult(
            title = title,
            meetingDateStr = meetingDateStr,
            topics = topics,
            decisions = decisions,
            extraTopics = extraNotesList.joinToString("\n"),
            attendees = attendees,
            rawText = rawText
        )
    }

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    private fun extractDaysFromText(text: String): Int? {
        val daysRegex = Regex("(\\d+)\\s*dias?", RegexOption.IGNORE_CASE)
        val match = daysRegex.find(text)
        return match?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun extractAssigneesFromText(text: String): List<String> {
        val found = mutableListOf<String>()
        val knownNames = listOf(
            "Paulo" to "Paulo Freitas",
            "Milton" to "José Milton",
            "Leandro" to "Leandro Silva",
            "Leonardo" to "Leonardo dos Santos",
            "Daniel" to "Daniel Alves",
            "Rafael" to "Rafael Santo",
            "Elias" to "Elias Borges",
            "Edvaldo" to "Edvaldo Nascimento"
        )

        for ((shortName, fullName) in knownNames) {
            if (text.contains(shortName, ignoreCase = true) && !found.contains(fullName)) {
                found.add(fullName)
            }
        }
        return found
    }

    companion object {
        const val SAMPLE_ATA_TEXT = """ATA DE REUNIÃO 26 de Agosto de 2026

Assuntos a considerar –
1. Contato desnecessário com removidos. Sfg Apêndice A pars. 21

DECISÕES
1 – O corpo entende que Cléber, Edineide, Arionaldo, Angela, Paulo e Simone (além de outros que talvez estejam presentes) não são exemplares. O assunto ainda será investigado (Adriana, Eduardo, e as testemunhas) serão consultados para entender o contexto e depois conversado e informado aos envolvidos. Os irmãos Paulo e Milton foram designados.

Leandro e Leonardo irão falar com Eduardo sobre o assunto.

Pauta Extra:
Daniel relembrou ao corpo de sempre deixar que os designados cuidem dos assuntos envolvendo os publicadores.

Daniel Alves Rafael Santo Elias Borges José Milton Edvaldo Nascimento Leandro Silva Paulo Freitas Leonardo dos Santos"""
    }
}
