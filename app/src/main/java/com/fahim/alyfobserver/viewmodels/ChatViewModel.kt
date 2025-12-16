package com.fahim.alyfobserver.viewmodels

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Log
import com.fahim.alyfobserver.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

data class Message(val text: String, val isUser: Boolean)

class ChatViewModel : ViewModel() {
    val messages = mutableStateListOf<Message>()
    private val client = OkHttpClient()
    private val geminiApiKey = BuildConfig.GEMINI_API_KEY

    init {
        // Add a demo conversation
        messages.add(Message("Hello! I'm the Observer AI. I can help you analyze conversations. What's on your mind?", false))
        messages.add(Message("I had a weird conversation with a friend. I'm not sure how to feel about it.", true))
        messages.add(Message("I can help with that. Can you paste the conversation here or describe the key points?", false))
        messages.add(Message("They said 'I'm fine' but their tone felt off. They seemed really distant.", true))
    }

    fun sendMessage(text: String) {
        messages.add(Message(text, true)) // Add user's message
        viewModelScope.launch {
            val prompt = """
                You are a helpful and friendly AI assistant. Respond to the following message in a conversational manner.

                User: "$text"
                AI:
            """.trimIndent()

            val aiResponse = makeApiCall(prompt)
            messages.add(Message(aiResponse, false)) // Add AI's response
        }
    }

    private suspend fun makeApiCall(prompt: String): String {
        return withContext(Dispatchers.IO) {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$geminiApiKey"
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
                .url(url)
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val errorBody = response.body?.string()
                        Log.e("ChatViewModel", "API call failed: ${response.code} - ${response.message} - $errorBody")
                        return@withContext "Error: Could not connect to AI."
                    }

                    val responseBody = response.body?.string()
                    val geminiData = JSONObject(responseBody)
                    val textContent = geminiData.optJSONArray("candidates")
                        ?.optJSONObject(0)
                        ?.optJSONObject("content")
                        ?.optJSONArray("parts")
                        ?.optJSONObject(0)
                        ?.optString("text", "Error: No response from AI.") as String

                    return@withContext textContent
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error making API call", e)
                return@withContext "Error: Could not connect to AI."
            }
        }
    }
}
