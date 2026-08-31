package com.example.data.model

data class ExtractedDecisionItem(
    val description: String,
    val assignees: List<String>,
    val deadlineDays: Int = 15,
    val deadlineDateStr: String = "",
    val topic: String = "",
    val priority: PriorityLevel = PriorityLevel.NORMAL,
    val decisionType: String = "FOLLOW_UP_ASSIGNMENT", // "FOLLOW_UP_ASSIGNMENT", "PERMANENT_PROCEDURE", "ACTION_DEADLINE"
    val typeLabel: String = "Atribuição de acompanhamento"
)

data class ExtractedAtaResult(
    val title: String,
    val meetingDateStr: String,
    val topics: List<String>,
    val decisions: List<ExtractedDecisionItem>,
    val extraTopics: String,
    val attendees: List<String>,
    val rawText: String
)
