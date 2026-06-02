package com.example.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String = "",
    val content: String = "",
    val checklistJson: String? = null, // JSON string for list of items [ { "text": "", "checked": false } ]
    val categoryId: Int? = null,
    val colorInt: Int = 0xFFF8F9FA.toInt(), // Default white-ish background color
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isFavorite: Boolean = false,
    val isLocked: Boolean = false,
    val lockPin: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis(),
    val voicePath: String? = null,
    val voiceDurationMs: Long = 0,
    val drawingPath: String? = null,
    val attachmentPath: String? = null, // PDF/Document path
    val attachmentName: String? = null,
    val tags: String = "" // List of comma-separated tags e.g. "work,personal"
) : Serializable
