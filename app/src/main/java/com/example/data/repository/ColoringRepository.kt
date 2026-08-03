package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import com.example.data.generator.ProceduralLineArtGenerator
import com.example.data.local.ColoringPageDao
import com.example.data.local.UserPreferencesRepository
import com.example.data.models.ColoringPage
import com.example.data.remote.GeminiImageService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class ColoringRepository(
    private val context: Context,
    private val dao: ColoringPageDao,
    private val userPrefs: UserPreferencesRepository,
    private val geminiService: GeminiImageService
) {
    val allPages: Flow<List<ColoringPage>> = dao.getAllPages()
    val favoritePages: Flow<List<ColoringPage>> = dao.getFavoritePages()

    suspend fun getPageById(id: String): ColoringPage? = dao.getPageById(id)

    suspend fun generatePages(
        prompt: String,
        category: String,
        styleName: String,
        batchCount: Int = 1
    ): List<ColoringPage> = withContext(Dispatchers.IO) {
        val createdPages = mutableListOf<ColoringPage>()
        val countToGenerate = if (userPrefs.isProUser.value) batchCount.coerceIn(1, 5) else 1

        for (i in 0 until countToGenerate) {
            val pageId = UUID.randomUUID().toString()
            val seed = System.currentTimeMillis() + i * 137L
            val currentPrompt = if (countToGenerate > 1) "$prompt Variation #${i + 1}" else prompt
            val title = if (prompt.length > 28) prompt.take(28) + "..." else prompt

            // Attempt AI image generation via Gemini API
            var bitmap: Bitmap? = geminiService.generateColoringPageImage(currentPrompt, styleName, category)

            // Fallback to procedural line-art generator if Gemini API is offline or unavailable
            if (bitmap == null) {
                bitmap = ProceduralLineArtGenerator.generateLineArtBitmap(
                    prompt = currentPrompt,
                    category = category,
                    style = styleName,
                    seed = seed
                )
            }

            // Save bitmap to app local storage
            val fileName = "coloring_${pageId}.png"
            val savedPath = ProceduralLineArtGenerator.saveBitmapToLocalFile(context, bitmap, fileName)

            val page = ColoringPage(
                id = pageId,
                title = if (title.isBlank()) "Adult Coloring Page" else title,
                prompt = currentPrompt,
                category = category,
                styleName = styleName,
                timestamp = System.currentTimeMillis() + i,
                imagePath = savedPath,
                isFavorite = false,
                seed = seed
            )

            dao.insertPage(page)
            createdPages.add(page)
        }

        // Record generation time for 1-minute free tier cooldown
        userPrefs.recordGenerationTime()

        createdPages
    }

    suspend fun toggleFavorite(id: String, isFavorite: Boolean) {
        dao.updateFavorite(id, isFavorite)
    }

    suspend fun deletePage(page: ColoringPage) {
        try {
            val file = File(page.imagePath)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        dao.deletePage(page)
    }
}
