package com.example.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class GeminiImageService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun generateColoringPageImage(
        userPrompt: String,
        styleName: String,
        category: String
    ): Bitmap? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isNull_or_blank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext null
        }

        val enrichedPrompt = buildString {
            append("High-quality adult coloring page line art: ")
            append(userPrompt)
            append(". Category: ").append(category)
            append(". Style: ").append(styleName)
            append(". Requirements: Intricate detailed crisp black vector lines on pure white paper background, high contrast printable line drawing, sharp edges, no gray shading, no color fill, professional coloring book illustration suitable for adult coloring.")
        }

        val jsonPayload = JSONObject().apply {
            val contentsArray = org.json.JSONArray().apply {
                val contentObj = JSONObject().apply {
                    val partsArray = org.json.JSONArray().apply {
                        val partObj = JSONObject().apply {
                            put("text", enrichedPrompt)
                        }
                        put(partObj)
                    }
                    put("parts", partsArray)
                }
                put(contentObj)
            }
            put("contents", contentsArray)

            val configObj = JSONObject().apply {
                val imageConfig = JSONObject().apply {
                    put("aspectRatio", "1:1")
                    put("imageSize", "1K")
                }
                put("imageConfig", imageConfig)
                val responseModalities = org.json.JSONArray().apply {
                    put("TEXT")
                    put("IMAGE")
                }
                put("responseModalities", responseModalities)
            }
            put("generationConfig", configObj)
        }

        val requestUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-image:generateContent?key=$apiKey"
        val requestBody = jsonPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(requestUrl)
            .post(requestBody)
            .build()

        try {
            val response = client.newCall(request).execute()
            val responseStr = response.body?.string() ?: return@withContext null
            if (!response.isSuccessful) {
                return@withContext null
            }

            val jsonResponse = JSONObject(responseStr)
            val candidates = jsonResponse.optJSONArray("candidates") ?: return@withContext null
            if (candidates.length() == 0) return@withContext null

            val candidate = candidates.getJSONObject(0)
            val content = candidate.optJSONObject("content") ?: return@withContext null
            val parts = content.optJSONArray("parts") ?: return@withContext null

            for (i in 0 until parts.length()) {
                val part = parts.getJSONObject(i)
                val inlineData = part.optJSONObject("inlineData")
                if (inlineData != null) {
                    val base64Data = inlineData.optString("data")
                    if (!base64Data.isNullOrEmpty()) {
                        val imageBytes = Base64.decode(base64Data, Base64.DEFAULT)
                        return@withContext BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    }
                }
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun String?.isNull_or_blank(): Boolean {
        return this == null || this.trim().isEmpty()
    }
}
