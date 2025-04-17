package com.gorai.myedenfocus.service

import android.content.Context
import android.util.Log
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
import java.io.File
import java.io.InputStream
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import org.apache.poi.xwpf.extractor.XWPFWordExtractor
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.hwpf.HWPFDocument
import org.apache.poi.hwpf.extractor.WordExtractor
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val TAG = "GeminiService"
    
    init {
        Log.d(TAG, "Initializing GeminiService")
        EnvConfig.init(context)
        validateApiKey()
        
        // As a fallback, try to save API key to SharedPreferences
        saveApiKeyToPrefs()
    }

    // Instead of using lazy initialization with an exception, get the API key when needed
    private fun getApiKey(): String {
        // First try to get from EnvConfig
        var key = EnvConfig.get("GEMINI_API_KEY")
        
        // If not found, try from SharedPreferences as fallback
        if (key.isNullOrBlank()) {
            Log.d(TAG, "API key not found in EnvConfig, trying SharedPreferences")
            key = context.getSharedPreferences("api_settings", Context.MODE_PRIVATE)
                .getString("GEMINI_API_KEY", null)
        }
        
        if (key.isNullOrBlank()) {
            Log.e(TAG, "API key is null or blank in both EnvConfig and SharedPreferences")
            throw IllegalStateException("API ERROR: API key not found or is blank")
        }
        
        return key
    }
    
    private fun saveApiKeyToPrefs() {
        try {
            // Try to manually read from .env file in root directory
            val rootEnvFile = File(".env")
            if (rootEnvFile.exists() && rootEnvFile.canRead()) {
                val content = rootEnvFile.readText()
                val regex = Regex("GEMINI_API_KEY=([^\\s]+)")
                val matchResult = regex.find(content)
                
                matchResult?.groupValues?.getOrNull(1)?.let { apiKey ->
                    if (apiKey.isNotBlank()) {
                        Log.d(TAG, "Extracted API key from .env file, saving to SharedPreferences")
                        // Save to SharedPreferences
                        context.getSharedPreferences("api_settings", Context.MODE_PRIVATE)
                            .edit()
                            .putString("GEMINI_API_KEY", apiKey)
                            .apply()
                    }
                }
            } else {
                // Try the hardcoded key from the file you shared earlier
                val hardcodedKey = "AIzaSyAErIRmuqDClgRmqAgnu1qsdnhQGi2oa7E"
                Log.d(TAG, "Setting hardcoded API key to SharedPreferences as last resort")
                context.getSharedPreferences("api_settings", Context.MODE_PRIVATE)
                    .edit()
                    .putString("GEMINI_API_KEY", hardcodedKey)
                    .apply()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving API key to SharedPreferences", e)
        }
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
        
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"

    private fun validateApiKey() {
        try {
            val key = EnvConfig.get("GEMINI_API_KEY")
            if (key.isNullOrBlank()) {
                Log.e(TAG, "API key is null or blank. Please check your .env file.")
            } else {
                Log.d(TAG, "API key found and not blank")
                // Log a few characters of the API key for verification (never log full API keys)
                val maskedKey = key.take(4) + "..." + key.takeLast(4)
                Log.d(TAG, "API key starts/ends with: $maskedKey")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error validating API key: ${e.message}")
        }
    }

    suspend fun extractSubjectsAndTopicsFromDocument(filePath: String): List<SubjectTopic> {
        val content = extractTextFromDocument(filePath)
        return extractSubjectsAndTopics(content)
    }
    
    suspend fun extractSubjectsAndTopicsFromInputStream(inputStream: InputStream): List<SubjectTopic> {
        val content = extractTextFromInputStream(inputStream)
        return extractSubjectsAndTopics(content)
    }
    
    private fun extractTextFromDocument(filePath: String): String {
        return when {
            filePath.endsWith(".pdf", ignoreCase = true) -> extractTextFromPdf(File(filePath))
            filePath.endsWith(".txt", ignoreCase = true) -> File(filePath).readText()
            filePath.endsWith(".docx", ignoreCase = true) -> extractTextFromDocx(File(filePath))
            filePath.endsWith(".doc", ignoreCase = true) -> extractTextFromDoc(File(filePath))
            else -> throw IllegalArgumentException("Unsupported file format")
        }
    }
    
    private fun extractTextFromInputStream(inputStream: InputStream): String {
        return try {
            // Attempt to parse as PDF first
            val pdfDoc = PDDocument.load(inputStream)
            val text = PDFTextStripper().getText(pdfDoc)
            pdfDoc.close()
            text
        } catch (e: Exception) {
            try {
                // If not a PDF, try reading as DOCX
                val docx = XWPFDocument(inputStream)
                val docxExtractor = XWPFWordExtractor(docx)
                val text = docxExtractor.text
                docxExtractor.close()
                docx.close()
                text
            } catch (e: Exception) {
                try {
                    // If not DOCX, try reading as DOC
                    val doc = HWPFDocument(inputStream)
                    val docExtractor = WordExtractor(doc)
                    val text = docExtractor.text
                    docExtractor.close()
                    doc.close()
                    text
                } catch (e: Exception) {
                    try {
                        // Finally try as plain text
                        inputStream.bufferedReader().use { it.readText() }
                    } catch (e: Exception) {
                        throw IllegalArgumentException("Unable to extract text from the provided document")
                    }
                }
            }
        }
    }
    
    private fun extractTextFromPdf(file: File): String {
        val document = PDDocument.load(file)
        val stripper = PDFTextStripper()
        val text = stripper.getText(document)
        document.close()
        return text
    }
    
    private fun extractTextFromDocx(file: File): String {
        val document = XWPFDocument(file.inputStream())
        val extractor = XWPFWordExtractor(document)
        val text = extractor.text
        extractor.close()
        document.close()
        return text
    }
    
    private fun extractTextFromDoc(file: File): String {
        val document = HWPFDocument(file.inputStream())
        val extractor = WordExtractor(document)
        val text = extractor.text
        extractor.close()
        document.close()
        return text
    }

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

            val apiKey = getApiKey() // Get the API key using our method
            
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
    
    suspend fun sendChatMessage(message: String, chatHistory: List<Pair<String, String>> = emptyList()): String {
        Log.d(TAG, "sendChatMessage called with message length: ${message.length}, history items: ${chatHistory.size}")
        
        try {
            val apiKey = getApiKey() // Get the API key directly when needed
            
            val contentsArray = JSONArray()
            
            // Add chat history
            if (chatHistory.isNotEmpty()) {
                val historyObject = JSONObject()
                val historyParts = JSONArray()
                
                for ((userMessage, aiResponse) in chatHistory) {
                    // Add user message
                    val userObject = JSONObject()
                    val userParts = JSONArray()
                    userParts.put(JSONObject().apply {
                        put("text", userMessage)
                    })
                    userObject.put("role", "user")
                    userObject.put("parts", userParts)
                    contentsArray.put(userObject)
                    
                    // Add AI response
                    val aiObject = JSONObject()
                    val aiParts = JSONArray()
                    aiParts.put(JSONObject().apply {
                        put("text", aiResponse)
                    })
                    aiObject.put("role", "model")
                    aiObject.put("parts", aiParts)
                    contentsArray.put(aiObject)
                }
            }
            
            // Add current user message
            val currentMessageObject = JSONObject()
            val currentMessageParts = JSONArray()
            currentMessageParts.put(JSONObject().apply {
                put("text", message)
            })
            currentMessageObject.put("role", "user")
            currentMessageObject.put("parts", currentMessageParts)
            contentsArray.put(currentMessageObject)
            
            val requestBody = JSONObject().apply {
                put("contents", contentsArray)
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                    put("maxOutputTokens", 1024)
                })
            }.toString()
            
            val fullUrl = "$baseUrl?key=$apiKey"
            Log.d(TAG, "Making API request to URL: ${fullUrl.substringBefore("?")}")
            
            val request = Request.Builder()
                .url(fullUrl)
                .post(requestBody.toRequestBody(jsonMediaType))
                .build()
                
            val response = withContext(Dispatchers.IO) {
                Log.d(TAG, "Executing API call")
                client.newCall(request).execute()
            }
            
            val responseBody = response.body?.string() ?: ""
            Log.d(TAG, "API response code: ${response.code}")
            
            if (!response.isSuccessful) {
                Log.e(TAG, "API call failed with code ${response.code}: $responseBody")
                throw Exception("API call failed with code ${response.code}: $responseBody")
            }
            
            Log.d(TAG, "API call successful, parsing response")
            
            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.getJSONArray("candidates")
            if (candidates.length() > 0) {
                val content = candidates.getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
                    .trim()
                    
                Log.d(TAG, "Successfully parsed response of length: ${content.length}")
                return content
            } else {
                Log.e(TAG, "No candidates found in the response")
                throw Exception("No valid response from API")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending chat message", e)
            throw Exception("Failed to get response: ${e.message}")
        }
    }
}

data class SubjectTopic(
    val subject: String,
    val topics: List<String>
) 