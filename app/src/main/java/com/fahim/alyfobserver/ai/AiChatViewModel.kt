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
                    Log.e("AiChatViewModel", "API Error: ${response.code} - $responseBody")
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
            Log.e("AiChatViewModel", "Exception: ${e.message}", e)
            return@withContext "Error: ${e.message}"
        }
    }
}
