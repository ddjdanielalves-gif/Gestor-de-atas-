package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "minutes")
data class MinuteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val meetingDate: String, // e.g. "26 de Agosto de 2026"
    val originalText: String,
    val topicsSummary: String,
    val extraNotes: String = "",
    val attendees: String = "", // Comma-separated list of attendees
    val createdAt: Long = System.currentTimeMillis()
)
