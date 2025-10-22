package com.fahim.alyfobserver

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException

object DataStore {
    private const val FILENAME = "data_rows.json"
    private val json = Json { prettyPrint = true }

    fun save(context: Context, dataRows: List<DataRow>) {
        val file = File(context.filesDir, FILENAME)
        try {
            val jsonString = json.encodeToString(dataRows)
            file.writeText(jsonString)
        } catch (e: IOException) {
            e.printStackTrace()
            // Handle error, e.g., log it or show a toast
        }
    }

    fun load(context: Context): List<DataRow> {
        val file = File(context.filesDir, FILENAME)
        if (!file.exists()) {
            return emptyList()
        }
        return try {
            val jsonString = file.readText()
            json.decodeFromString<List<DataRow>>(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
            // Handle error, e.g., log it or return a default empty list
            emptyList()
        }
    }
}