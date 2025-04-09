package com.gorai.myedenfocus.service

import android.content.Context
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.gorai.myedenfocus.util.EnvConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    init {
        EnvConfig.init(context)
    }

    private val apiKey: String by lazy {
        EnvConfig.get("GEMINI_API_KEY") ?: throw IllegalStateException("API ERROR")
    }

    private val client = OkHttpClient()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"

    suspend fun extractSubjectsAndTopics(pdfContent: String): List<SubjectTopic> {
        try {
            val prompt = """
                Extract the list of subjects and their topics from the following syllabus text. 
                Format the response as a JSON array where each object has:
                - "subject": The name of the subject
                - "topics": An array of topics covered in that subject
                
                Example format:
                [
                  {
                    "subject": "Mathematics",
                    "topics": ["Calculus", "Linear Algebra", "Statistics"]
                  },
                  {
                    "subject": "Computer Science",
                    "topics": ["Algorithms", "Data Structures", "System Design"]
                  }
                ]
                
                Here is the syllabus content:
                $pdfContent
            """.trimIndent()

            val requestBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
            }.toString()

            val request = Request.Builder()
                .url("$baseUrl?key=$apiKey")
                .post(requestBody.toRequestBody(jsonMediaType))
                .build()

            val response = withContext(Dispatchers.IO) {
                client.newCall(request).execute()
            }

            val responseBody = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                throw Exception("API call failed: $responseBody")
            }

            // Parse the Gemini API response
            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.getJSONArray("candidates")
            if (candidates.length() > 0) {
                val content = candidates.getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
                    .trim()

                // Extract JSON from response if needed
                val jsonStart = content.indexOf("[")
                val jsonEnd = content.lastIndexOf("]") + 1
                
                return if (jsonStart >= 0 && jsonEnd > jsonStart) {
                    val jsonContent = content.substring(jsonStart, jsonEnd)
                    parseSubjectTopicsJson(jsonContent)
                } else {
                    emptyList()
                }
            }
            return emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
    }
    
    private fun parseSubjectTopicsJson(jsonContent: String): List<SubjectTopic> {
        return try {
            val gson = com.google.gson.Gson()
            gson.fromJson(jsonContent, Array<SubjectTopic>::class.java).toList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}

data class SubjectTopic(
    val subject: String,
    val topics: List<String>
) 