package com.fahim.alyfobserver.ai

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fahim.alyfobserver.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

class AiChatViewModel : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // --- Copied from ammu project for Test AI functionality ---
    private val _testLogOutput = MutableStateFlow("")
    val testLogOutput: StateFlow<String> = _testLogOutput.asStateFlow()
    private val _detailedErrorLogs = MutableStateFlow<Map<String, String>>(emptyMap())
    // ---

    private val GEMINI_API_KEY = BuildConfig.GEMINI_API_KEY
    private val GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$GEMINI_API_KEY"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun sendMessage(userText: String) {
        if (userText.isBlank()) return

        val userMessage = ChatMessage(text = userText, isUser = true)
        _messages.value = _messages.value + userMessage
        _isLoading.value = true

        viewModelScope.launch {
            val responseText = generateContent(userText)
            val aiMessage = ChatMessage(text = responseText, isUser = false)
            _messages.value = _messages.value + aiMessage
            _isLoading.value = false
        }
    }

    private suspend fun generateContent(prompt: String): String = withContext(Dispatchers.IO) {
        try {
            Log.d("AiChatViewModel", "Sending request to Gemini. URL: $GEMINI_API_URL")
            
            val jsonBody = JSONObject().apply {
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
                .url(GEMINI_API_URL)
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()
                Log.d("AiChatViewModel", "Response Code: ${response.code}")
                
                if (!response.isSuccessful) {
                    Log.e("AiChatViewModel", "Gemini API Error: ${response.code} - $responseBody")
                    return@withContext "Error: ${response.code} - ${response.message}\n$responseBody"
                }

                if (responseBody == null) return@withContext "Empty response"
                val jsonResponse = JSONObject(responseBody)
                
                val textContent = jsonResponse.optJSONArray("candidates")
                    ?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text")

                return@withContext textContent ?: "No content generated."
            }
        } catch (e: Exception) {
            Log.e("AiChatViewModel", "Gemini API Exception: ${e.message}", e)
            return@withContext "Error: ${e.message}"
        }
    }

    suspend fun generateReplyFromConversation(messages: List<com.fahim.alyfobserver.Message>): String {
        val conversationHistory = messages.joinToString(separator = "\n") {
            if (it.isFromUser) "Friend: ${it.text}" else "Me: ${it.text}"
        }

        val prompt = """
            You are a helpful assistant. Based on the following conversation history, please generate a short, appropriate, and friendly reply.

            --- Conversation History ---
            $conversationHistory
            --- End of History ---

            Your reply:
        """.trimIndent()

        return generateContent(prompt)
    }

    // --- Copied from ammu project for Test AI functionality ---
    fun runTestAiCall() {
        viewModelScope.launch {
            _testLogOutput.value = "" // Clear previous log
            _testLogOutput.value += "Starting AI test call...\n"
            _testLogOutput.value += "Using API Key ending in: ...${GEMINI_API_KEY.takeLast(4)}\n"
            try {
                val dummyComments = listOf(
                    ClassifiedComment(id = "test_1", text = "This is a test comment about a product issue.", from = "User A"),
                    ClassifiedComment(id = "test_2", text = "Great service, thank you!", from = "User B"),
                    ClassifiedComment(id = "test_3", text = "Where is my order? It's late!", from = "User C")
                )
                _testLogOutput.value += "Dummy comments prepared: ${dummyComments.size}\n"

                val analyzedComments = analyzeCommentsWithGeminiAPI(dummyComments)
                _testLogOutput.value += "AI analysis completed. Results:\n"
                analyzedComments.forEach {
                    _testLogOutput.value += "  ID: ${it.id}, Priority: ${it.priority}, Reason: ${it.reason}, Reply: ${it.reply}\n"
                }
                _testLogOutput.value += "AI test call finished successfully.\n"
            } catch (e: Exception) {
                val errorMessage = "AI test call failed: ${e.message}\n"
                _testLogOutput.value += errorMessage
                Log.e("AiChatViewModel", errorMessage, e)
            }
        }
    }

    private suspend fun analyzeCommentsWithGeminiAPI(rawComments: List<ClassifiedComment>): List<ClassifiedComment> = withContext(Dispatchers.IO) {
        Log.d("AiChatViewModel", "analyzeCommentsWithGeminiAPI called with ${rawComments.size} comments.")
        rawComments.forEachIndexed { index, comment ->
            Log.d("AiChatViewModel", "AI Input Comment ${index + 1}: ID=${comment.id}, Text='${comment.text.take(50)}...'")
        }
        val analyzedComments = mutableListOf<ClassifiedComment>()
        val BATCH_SIZE = 10 // Define batch size

        if (rawComments.isEmpty()) {
            return@withContext emptyList()
        }

        rawComments.chunked(BATCH_SIZE).forEachIndexed { index, batch ->
            Log.d("AiChatViewModel", "Processing batch ${index + 1}/${rawComments.chunked(BATCH_SIZE).size} with ${batch.size} comments.")
            // Construct a single prompt for the current batch of comments
            val commentsForPrompt = batch.joinToString(separator = "---") { 
                "Comment ID: ${it.id}\nComment Text: ${it.text}"
            }
            // Store the AI input prompt for each comment in the batch for debugging
            batch.forEach { comment ->
                _detailedErrorLogs.value = _detailedErrorLogs.value + (comment.id to "AI Input Prompt:\n${commentsForPrompt}")
            }

            val prompt = """
                You are a professional social media manager for a busy brand. Your task is to analyze the following Facebook comments and classify each one based on urgency and content.

                For each comment provided below, return a JSON object with four fields:
                1.  "id": The original Comment ID.
                2.  "priority": "High", "Medium", or "Low" based on the urgency of the comment.
                3.  "reason": A brief, one-sentence explanation for your classification.
                4.  "reply": A short, friendly, and appropriate reply to the comment. For generic comments, you can use emojis.

                Return a JSON array containing one such object for each comment.

                --- Comments to Analyze ---
                $commentsForPrompt
                --- End of Comments ---
                """.trimIndent()

            val geminiRequestBody = JSONObject().apply {
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
                .url(GEMINI_API_URL) // Use the direct Gemini API URL
                .post(geminiRequestBody.toRequestBody("application/json".toMediaType()))
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    Log.d("AiChatViewModel", "Gemini API response for batch ${index + 1}: ${response.code}, successful: ${response.isSuccessful}")
                    if (!response.isSuccessful) {
                        val errorBody = response.body?.string()
                        val detailedLog = "Gemini API call failed for batch ${index + 1}: ${response.code} - ${response.message} - $errorBody"
                        Log.e("AiChatViewModel", detailedLog)
                        batch.forEach { _detailedErrorLogs.value = _detailedErrorLogs.value + (it.id to detailedLog) }
                        analyzedComments.addAll(batch.map { it.copy(priority = "Error", reason = "Gemini API error: ${response.code}. See detailed logs.") })
                        return@use // Continue to next batch
                    }

                    val responseBody = response.body?.string()
                    Log.d("AiChatViewModel", "Gemini API response body length for batch ${index + 1}: ${responseBody?.length ?: 0}")
                    val geminiData = JSONObject(responseBody)
                    val textContent = geminiData.optJSONArray("candidates")
                        ?.optJSONObject(0)
                        ?.optJSONObject("content")
                        ?.optJSONArray("parts")
                        ?.optJSONObject(0)
                        ?.optString("text") as String? ?: ""

                    val cleanedJson = textContent.trim().removePrefix("```json\n").removeSuffix("\n```")
                    Log.d("AiChatViewModel", "Cleaned JSON content length for batch ${index + 1}: ${cleanedJson.length}")

                    try {
                        val classifiedCommentsArray = JSONArray(cleanedJson)
                        Log.d("AiChatViewModel", "Received ${classifiedCommentsArray.length()} classified comments from Gemini for batch ${index + 1}.")
                        val classifiedMap = mutableMapOf<String, ClassifiedComment>()

                        for (i in 0 until classifiedCommentsArray.length()) {
                            val classifiedCommentJson = classifiedCommentsArray.getJSONObject(i)
                            val id: String = classifiedCommentJson.optString("id", "")
                            val priority: String = classifiedCommentJson.optString("priority", "Low")
                            val reason: String = classifiedCommentJson.optString("reason", "N/A")
                            val reply: String = classifiedCommentJson.optString("reply", "")

                            val originalComment = batch.find { it.id == id }
                            if (originalComment != null) {
                                classifiedMap[id] = originalComment.copy(priority = priority, reason = reason, reply = reply)
                            } else {
                                Log.w("AiChatViewModel", "Gemini returned classification for unknown comment ID: $id in batch ${index + 1}")
                            }
                        }
                        analyzedComments.addAll(batch.map {
                            classifiedMap[it.id] ?: it.copy(priority = "Error", reason = "Classification missing from AI in batch ${index + 1}")
                        })
                        Log.d("AiChatViewModel", "Batch ${index + 1} processed. Total analyzed comments so far: ${analyzedComments.size}")

                    } catch (jsonParseError: JSONException) {
                        val detailedLog = "Error parsing Gemini response JSON for batch ${index + 1}: ${jsonParseError.message}\n${Log.getStackTraceString(jsonParseError)}"
                        Log.e("AiChatViewModel", detailedLog, jsonParseError)
                        batch.forEach { _detailedErrorLogs.value = _detailedErrorLogs.value + (it.id to detailedLog) }
                        analyzedComments.addAll(batch.map { it.copy(priority = "Error", reason = "Failed to parse AI response. See detailed logs.") })
                    }
                }
            } catch (e: Exception) {
                val detailedLog = "Exception during Gemini API call for batch ${index + 1}: ${e.message}\n${Log.getStackTraceString(e)}"
                Log.e("AiChatViewModel", detailedLog, e)
                batch.forEach { _detailedErrorLogs.value = _detailedErrorLogs.value + (it.id to detailedLog) }
                analyzedComments.addAll(batch.map { it.copy(priority = "Error", reason = "Network or API error. See detailed logs.") })
            }
        }
        return@withContext analyzedComments
    }
    // ---
}
