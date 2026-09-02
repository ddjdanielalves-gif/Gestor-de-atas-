package com.example.service

import com.example.BuildConfig
import com.example.data.model.ExtractedAtaResult
import com.example.data.model.ExtractedDecisionItem
import com.example.data.model.PriorityLevel
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
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun parseMinuteText(text: String): ExtractedAtaResult = withContext(Dispatchers.IO) {
        require(text.isNotBlank()) { "O texto da ata está vazio." }

        val apiKey = runCatching {
            BuildConfig.GEMINI_API_KEY
        }.getOrDefault("")

        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            runCatching {
                callGeminiApi(text, apiKey)
            }.getOrNull()
                ?.takeIf {
                    it.decisions.isNotEmpty() || it.topics.isNotEmpty()
                }
                ?.let {
                    return@withContext it
                }
        }

        parseLocalMinuteText(text)
    }

    private fun callGeminiApi(
        rawText: String,
        apiKey: String
    ): ExtractedAtaResult? {

        val prompt = """
            Você é um especialista em leitura de atas e gestão de decisões.

            Analise a ATA COMPLETA abaixo e extraia somente informações que
            estejam presentes ou que possam ser determinadas diretamente pelo texto.

            OBJETIVO:

            Para cada decisão ou ação que exija acompanhamento, identifique:

            - assunto relacionado;
            - o que foi decidido ou precisa ser feito;
            - quem ficou responsável;
            - prazo, quando houver.

            REGRAS OBRIGATÓRIAS:

            1. NÃO invente nomes.
               Se o responsável não estiver claramente indicado,
               retorne assignees como lista vazia.

            2. NÃO invente datas.

            3. Se uma ação tiver prazo explícito em dias:
               "em 7 dias"
               retorne deadlineDays=7.

            4. Se houver uma data explícita:
               "até 15/09/2026"
               retorne essa data em deadlineDateStr no formato YYYY-MM-DD.

            5. Se houver prazo relativo:
               "em duas semanas"
               converta para 14 dias.

            6. Se NÃO houver prazo para uma decisão acionável,
               retorne:

               deadlineDays=15
               deadlineProvided=false

               Isso significa que o aplicativo aplicará automaticamente
               o prazo padrão de 15 dias A PARTIR DA DATA DA ATA.

            7. Só use PERMANENT_PROCEDURE se a ata afirmar claramente
               que a regra, orientação ou procedimento é permanente/contínuo
               e não é uma ação a ser concluída.

            8. Para decisões permanentes:
               deadlineDays=0
               deadlineDateStr=""

            9. "URGENTE" só deve virar prioridade URGENT se a ata indicar
               claramente urgência.

            10. Use HIGH somente quando a ata indicar claramente uma
                prioridade maior que normal.

            11. Separe ações diferentes em decisões diferentes quando
                houver responsáveis ou tarefas diferentes.

            12. Não trate simplesmente a lista de participantes como
                responsáveis.

            13. Não crie decisões a partir de comentários que não gerem
                ação, deliberação, orientação permanente ou acompanhamento.

            TIPOS:

            FOLLOW_UP_ASSIGNMENT:
            ação/acompanhamento sem prazo explícito.
            Aplica automaticamente 15 dias.

            ACTION_DEADLINE:
            ação com prazo/data explícitos.

            PERMANENT_PROCEDURE:
            regra explicitamente permanente/contínua.

            A DATA DA ATA É FUNDAMENTAL.

            Extraia-a com precisão.

            Retorne meetingDateStr no formato:

            YYYY-MM-DD

            JSON esperado:

            {
              "title": "...",
              "meetingDateStr": "YYYY-MM-DD",
              "topics": ["..."],
              "decisions": [
                {
                  "description": "...",
                  "assignees": ["Nome 1"],
                  "decisionType": "FOLLOW_UP_ASSIGNMENT",
                  "deadlineDays": 15,
                  "deadlineDateStr": "",
                  "deadlineProvided": false,
                  "topic": "...",
                  "priority": "NORMAL"
                }
              ],
              "extraTopics": "...",
              "attendees": ["..."]
            }

            ATA:

            $rawText
        """.trimIndent()

        val schema = JSONObject().apply {
            put("type", "OBJECT")

            put(
                "properties",
                JSONObject().apply {

                    put("title", stringSchema())

                    put("meetingDateStr", stringSchema())

                    put(
                        "topics",
                        arrayOfStringsSchema()
                    )

                    put(
                        "decisions",
                        JSONObject().apply {

                            put("type", "ARRAY")

                            put(
                                "items",
                                JSONObject().apply {

                                    put("type", "OBJECT")

                                    put(
                                        "properties",
                                        JSONObject().apply {

                                            put(
                                                "description",
                                                stringSchema()
                                            )

                                            put(
                                                "assignees",
                                                arrayOfStringsSchema()
                                            )

                                            put(
                                                "decisionType",
                                                JSONObject().apply {
                                                    put("type", "STRING")
                                                    put(
                                                        "enum",
                                                        JSONArray(
                                                            listOf(
                                                                "FOLLOW_UP_ASSIGNMENT",
                                                                "ACTION_DEADLINE",
                                                                "PERMANENT_PROCEDURE"
                                                            )
                                                        )
                                                    )
                                                }
                                            )

                                            put(
                                                "deadlineDays",
                                                JSONObject().apply {
                                                    put("type", "INTEGER")
                                                }
                                            )

                                            put(
                                                "deadlineDateStr",
                                                stringSchema()
                                            )

                                            put(
                                                "deadlineProvided",
                                                JSONObject().apply {
                                                    put("type", "BOOLEAN")
                                                }
                                            )

                                            put(
                                                "topic",
                                                stringSchema()
                                            )

                                            put(
                                                "priority",
                                                JSONObject().apply {
                                                    put("type", "STRING")
                                                    put(
                                                        "enum",
                                                        JSONArray(
                                                            listOf(
                                                                "NORMAL",
                                                                "HIGH",
                                                                "URGENT"
                                                            )
                                                        )
                                                    )
                                                }
                                            )
                                        }
                                    )

                                    put(
                                        "required",
                                        JSONArray(
                                            listOf(
                                                "description",
                                                "assignees",
                                                "decisionType",
                                                "deadlineDays",
                                                "deadlineDateStr",
                                                "deadlineProvided",
                                                "topic",
                                                "priority"
                                            )
                                        )
                                    )
                                }
                            )
                        }
                    )

                    put(
                        "extraTopics",
                        stringSchema()
                    )

                    put(
                        "attendees",
                        arrayOfStringsSchema()
                    )
                }
            )

            put(
                "required",
                JSONArray(
                    listOf(
                        "title",
                        "meetingDateStr",
                        "topics",
                        "decisions",
                        "extraTopics",
                        "attendees"
                    )
                )
            )
        }

        val body = JSONObject().apply {

            put(
                "contents",
                JSONArray().put(
                    JSONObject().put(
                        "parts",
                        JSONArray().put(
                            JSONObject().put(
                                "text",
                                prompt
                            )
                        )
                    )
                )
            )

            put(
                "generationConfig",
                JSONObject().apply {

                    put(
                        "responseMimeType",
                        "application/json"
                    )

                    put(
                        "responseSchema",
                        schema
                    )

                    put(
                        "maxOutputTokens",
                        12000
                    )
                }
            )
        }

        val request = Request.Builder()
            .url(
                "https://generativelanguage.googleapis.com/" +
                    "v1beta/models/gemini-3.5-flash:generateContent"
            )
            .addHeader(
                "x-goog-api-key",
                apiKey
            )
            .post(
                body.toString()
                    .toRequestBody(
                        "application/json; charset=utf-8".toMediaType()
                    )
            )
            .build()

        client.newCall(request).execute().use { response ->

            if (!response.isSuccessful) {
                return null
            }

            val responseText =
                response.body?.string().orEmpty()

            if (responseText.isBlank()) {
                return null
            }

            val root =
                JSONObject(responseText)

            val text =
                root.optJSONArray("candidates")
                    ?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.let { parts ->

                        buildString {

                            for (i in 0 until parts.length()) {

                                append(
                                    parts
                                        .optJSONObject(i)
                                        ?.optString("text")
                                        .orEmpty()
                                )
                            }
                        }
                    }
                    .orEmpty()
                    .trim()

            if (text.isBlank()) {
                return null
            }

            val cleanJson =
                text
                    .removePrefix("```json")
                    .removePrefix("```")
                    .removeSuffix("```")
                    .trim()

            parseGeminiJson(
                JSONObject(cleanJson),
                rawText
            )
        }
    }

    private fun stringSchema(): JSONObject =
        JSONObject().put(
            "type",
            "STRING"
        )

    private fun arrayOfStringsSchema(): JSONObject =
        JSONObject().apply {

            put(
                "type",
                "ARRAY"
            )

            put(
                "items",
                JSONObject().put(
                    "type",
                    "STRING"
                )
            )
        }

    private fun parseGeminiJson(
        json: JSONObject,
        rawText: String
    ): ExtractedAtaResult {

        val title =
            json.optString("title")
                .ifBlank {
                    "Ata de Reunião"
                }

        val meetingDateStr =
            json.optString("meetingDateStr")
                .ifBlank {
                    "Data não identificada"
                }

        val topics =
            json.optJSONArray("topics")
                .toStringList()

        val attendees =
            json.optJSONArray("attendees")
                .toStringList()

        val extraTopics =
            json.optString(
                "extraTopics",
                ""
            )

        val decisions =
            mutableListOf<ExtractedDecisionItem>()

        val decisionsArray =
            json.optJSONArray("decisions")

        if (decisionsArray != null) {

            for (i in 0 until decisionsArray.length()) {

                val obj =
                    decisionsArray.optJSONObject(i)
                        ?: continue

                val description =
                    obj.optString("description")
                        .trim()

                if (description.isBlank()) {
                    continue
                }

                val type =
                    when (
                        obj.optString(
                            "decisionType"
                        ).uppercase()
                    ) {

                        "ACTION_DEADLINE" ->
                            "ACTION_DEADLINE"

                        "PERMANENT_PROCEDURE" ->
                            "PERMANENT_PROCEDURE"

                        else ->
                            "FOLLOW_UP_ASSIGNMENT"
                    }

                val deadlineDaysFromAi =
                    obj.optInt(
                        "deadlineDays",
                        0
                    )

                val deadlineProvided =
                    obj.optBoolean(
                        "deadlineProvided",
                        false
                    )

                val deadlineDateStr =
                    obj.optString(
                        "deadlineDateStr",
                        ""
                    ).trim()

                val normalizedDays =
                    when {

                        type == "PERMANENT_PROCEDURE" ->
                            0

                        deadlineProvided &&
                            deadlineDaysFromAi > 0 ->
                            deadlineDaysFromAi

                        deadlineProvided &&
                            deadlineDateStr.isNotBlank() ->
                            0

                        else ->
                            15
                    }

                val priority =
                    when (
                        obj.optString(
                            "priority",
                            "NORMAL"
                        ).uppercase()
                    ) {

                        "URGENT" ->
                            PriorityLevel.URGENT

                        "HIGH" ->
                            PriorityLevel.HIGH

                        else ->
                            PriorityLevel.NORMAL
                    }

                val label =
                    when (type) {

                        "ACTION_DEADLINE" ->
                            "Ação com prazo"

                        "PERMANENT_PROCEDURE" ->
                            "Decisão permanente de procedimento"

                        else ->
                            "Atribuição de acompanhamento"
                    }

                decisions +=
                    ExtractedDecisionItem(

                        description =
                            description,

                        assignees =
                            obj
                                .optJSONArray("assignees")
                                .toStringList(),

                        deadlineDays =
                            normalizedDays,

                        deadlineDateStr =
                            deadlineDateStr,

                        topic =
                            obj
                                .optString(
                                    "topic",
                                    ""
                                )
                                .trim(),

                        priority =
                            priority,

                        decisionType =
                            type,

                        typeLabel =
                            label
                    )
            }
        }

        return ExtractedAtaResult(

            title =
                title,

            meetingDateStr =
                meetingDateStr,

            topics =
                topics,

            decisions =
                decisions,

            extraTopics =
                extraTopics,

            attendees =
                attendees,

            rawText =
                rawText
        )
    }

    fun parseLocalMinuteText(
        rawText: String
    ): ExtractedAtaResult {

        val lines =
            rawText
                .lines()
                .map { it.trim() }
                .filter { it.isNotBlank() }

        val dateRegex =
            Regex(
                "(\\d{1,2}\\s+de\\s+[a-zA-ZçÇ]+\\s+de\\s+\\d{4})" +
                    "|(\\d{1,2}/\\d{1,2}/\\d{4})" +
                    "|(\\d{4}-\\d{2}-\\d{2})",
                RegexOption.IGNORE_CASE
            )

        val meetingDate =
            lines
                .asSequence()
                .mapNotNull {
                    dateRegex
                        .find(it)
                        ?.value
                }
                .firstOrNull()
                ?: "Data não identificada"

        val topics =
            mutableListOf<String>()

        val decisionsRaw =
            mutableListOf<String>()

        val extras =
            mutableListOf<String>()

        var section = ""

        for (line in lines) {

            val upper =
                line.uppercase()

            when {

                upper.startsWith(
                    "ASSUNTOS A CONSIDERAR"
                ) ||
                    upper.startsWith("ASSUNTOS:") -> {

                    section = "topics"
                    continue
                }

                upper.startsWith("DECISÕES") ||
                    upper.startsWith("DECISOES") ||
                    upper.startsWith("DECISÃO") -> {

                    section = "decisions"
                    continue
                }

                upper.startsWith("PAUTA EXTRA") ||
                    upper.startsWith("OUTROS ASSUNTOS") -> {

                    section = "extras"
                    continue
                }
            }

            when (section) {

                "topics" ->
                    topics +=
                        line
                            .removePrefix("-")
                            .replace(
                                Regex(
                                    "^\\d+[.)-]\\s*"
                                ),
                                ""
                            )
                            .trim()

                "decisions" ->
                    decisionsRaw += line

                "extras" ->
                    extras += line
            }
        }

        val blocks =
            mutableListOf<String>()

        var current =
            StringBuilder()

        for (line in decisionsRaw) {

            val startsNumbered =
                line.matches(
                    Regex(
                        "^\\d+\\s*[.)–-]\\s*.*"
                    )
                )

            if (
                startsNumbered &&
                current.isNotBlank()
            ) {

                blocks +=
                    current
                        .toString()
                        .trim()

                current =
                    StringBuilder()
            }

            current
                .append(line)
                .append(' ')
        }

        if (current.isNotBlank()) {

            blocks +=
                current
                    .toString()
                    .trim()
        }

        if (blocks.isEmpty()) {
            blocks += decisionsRaw
        }

        val decisions =
            blocks
                .filter {
                    it.isNotBlank()
                }
                .map { raw ->

                    val description =
                        raw
                            .replace(
                                Regex(
                                    "^\\d+\\s*[.)–-]\\s*"
                                ),
                                ""
                            )
                            .trim()

                    val explicitDays =
                        extractDaysFromText(
                            description
                        )

                    val explicitDate =
                        extractExplicitDate(
                            description
                        )

                    val permanent =
                        description.contains(
                            "permanente",
                            true
                        ) ||
                            description.contains(
                                "contínuo",
                                true
                            )

                    ExtractedDecisionItem(

                        description =
                            description,

                        assignees =
                            extractExplicitAssignees(
                                description
                            ),

                        deadlineDays =
                            when {
                                permanent -> 0
                                explicitDays != null ->
                                    explicitDays
                                else -> 15
                            },

                        deadlineDateStr =
                            explicitDate ?: "",

                        topic =
                            topics.firstOrNull()
                                .orEmpty(),

                        priority =
                            if (
                                description.contains(
                                    "urgente",
                                    true
                                )
                            ) {
                                PriorityLevel.URGENT
                            } else {
                                PriorityLevel.NORMAL
                            },

                        decisionType =
                            when {
                                permanent ->
                                    "PERMANENT_PROCEDURE"

                                explicitDays != null ||
                                    explicitDate != null ->
                                    "ACTION_DEADLINE"

                                else ->
                                    "FOLLOW_UP_ASSIGNMENT"
                            },

                        typeLabel =
                            when {
                                permanent ->
                                    "Decisão permanente de procedimento"

                                explicitDays != null ||
                                    explicitDate != null ->
                                    "Ação com prazo"

                                else ->
                                    "Atribuição de acompanhamento"
                            }
                    )
                }

        return ExtractedAtaResult(

            title =
                "Ata de Reunião • $meetingDate",

            meetingDateStr =
                meetingDate,

            topics =
                topics.ifEmpty {
                    listOf(
                        "Reunião Geral e Deliberações"
                    )
                },

            decisions =
                decisions,

            extraTopics =
                extras.joinToString("\n"),

            attendees =
                emptyList(),

            rawText =
                rawText
        )
    }

    private fun extractDaysFromText(
        text: String
    ): Int? {

        Regex(
            "(?:em|dentro de|no prazo de)\\s+(\\d+)\\s+dias?",
            RegexOption.IGNORE_CASE
        )
            .find(text)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
            ?.let {
                return it
            }

        Regex(
            "\\b(\\d+)\\s+dias?\\b",
            RegexOption.IGNORE_CASE
        )
            .find(text)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
            ?.let {
                return it
            }

        Regex(
            "\\b(\\d+)\\s+semanas?\\b",
            RegexOption.IGNORE_CASE
        )
            .find(text)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
            ?.let {
                return it * 7
            }

        return null
    }

    private fun extractExplicitDate(
        text: String
    ): String? {

        Regex(
            "\\b\\d{1,2}/\\d{1,2}/\\d{4}\\b"
        )
            .find(text)
            ?.value
            ?.let {
                return it
            }

        Regex(
            "\\b\\d{4}-\\d{2}-\\d{2}\\b"
        )
            .find(text)
            ?.value
            ?.let {
                return it
            }

        return null
    }

    private fun extractExplicitAssignees(
        text: String
    ): List<String> {

        val match =
            Regex(
                "(?:respons[aá]vel(?:is)?|designad(?:o|a|os|as))" +
                    "\\s*[:\\-]?\\s*([^.;\\n]+)",
                RegexOption.IGNORE_CASE
            )
                .find(text)
                ?: return emptyList()

        return match
            .groupValues[1]
            .replace(
                " e ",
                ",",
                ignoreCase = true
            )
            .split(',')
            .map {
                it.trim()
                    .removePrefix("o ")
                    .removePrefix("a ")
            }
            .filter {
                it.length >= 2
            }
            .distinct()
    }

    private fun JSONArray?.toStringList(): List<String> {

        if (this == null) {
            return emptyList()
        }

        return (0 until length())
            .mapNotNull {
                optString(
                    it,
                    ""
                )
                    .trim()
                    .takeIf(
                        String::isNotBlank
                    )
            }
    }
}
