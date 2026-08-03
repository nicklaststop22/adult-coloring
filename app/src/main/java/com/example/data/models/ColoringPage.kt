package com.example.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "coloring_pages")
data class ColoringPage(
    @PrimaryKey val id: String,
    val title: String,
    val prompt: String,
    val category: String,
    val styleName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val imagePath: String, // Local file path or data URI
    val isFavorite: Boolean = false,
    val seed: Long = 0L,
    val width: Int = 1024,
    val height: Int = 1024
)

data class StylePreset(
    val id: String,
    val name: String,
    val description: String,
    val promptModifier: String
)

data class CategoryPreset(
    val id: String,
    val name: String,
    val iconName: String,
    val samplePrompts: List<String>
)
