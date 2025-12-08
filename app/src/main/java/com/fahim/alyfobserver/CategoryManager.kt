package com.fahim.alyfobserver

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

enum class ConversationCategory {
    DEFAULT, NEW, VIP, REGULAR
}

@Serializable
private data class CategoryData(val categories: Map<String, ConversationCategory>)

class CategoryManager(private val context: Context) {
    private val categoriesFile = File(context.filesDir, "conversation_categories.json")
    private var categoryMap = mutableMapOf<String, ConversationCategory>()

    init {
        loadCategories()
    }

    private fun loadCategories() {
        if (categoriesFile.exists()) {
            try {
                val jsonString = categoriesFile.readText()
                val decodedData = Json.decodeFromString<CategoryData>(jsonString)
                categoryMap.putAll(decodedData.categories)
            } catch (e: Exception) {
                // Handle exceptions like file corruption or parsing errors
                e.printStackTrace()
            }
        }
    }

    private fun saveCategories() {
        try {
            val dataToSave = CategoryData(categoryMap)
            val jsonString = Json.encodeToString(dataToSave)
            categoriesFile.writeText(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getCategoryForSender(sender: String): ConversationCategory {
        return categoryMap[sender] ?: ConversationCategory.DEFAULT
    }

    fun setCategoryForSender(sender: String, category: ConversationCategory) {
        if (category == ConversationCategory.DEFAULT) {
            categoryMap.remove(sender)
        } else {
            categoryMap[sender] = category
        }
        saveCategories()
    }
    
    fun getAllCategories(): Map<String, ConversationCategory> {
        return categoryMap.toMap()
    }
}
