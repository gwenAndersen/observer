package com.fahim.alyfobserver

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer

val json = Json { ignoreUnknownKeys = true }

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "observer_datastore")

private val BUTTON_LAYOUT_KEY = stringPreferencesKey("button_layout")
private val HEART_BUTTON_LAYOUT_KEY = stringPreferencesKey("heart_button_layout")
private val DRAGGABLE_BUTTONS_KEY = stringPreferencesKey("draggable_buttons")
private val DATA_ROWS_KEY = stringPreferencesKey("data_rows")

@Serializable
data class ButtonConfig(
    val id: String,
    val text: String,
    val emoji: String? = ""
)

@Serializable
data class DraggableButtonConfig(
    val id: String,
    val text: String,
    val emoji: String,
    var x: Float,
    var y: Float
)

object DataStoreManager {

    suspend fun saveDraggableButtons(context: Context, buttons: List<DraggableButtonConfig>) {
        val json = Json.encodeToString(buttons)
        context.dataStore.edit {
            preferences ->
            preferences[DRAGGABLE_BUTTONS_KEY] = json
        }
    }

    suspend fun loadDraggableButtons(context: Context): List<DraggableButtonConfig> {
        val json = context.dataStore.data.first()[DRAGGABLE_BUTTONS_KEY]
        return if (json != null) {
            Json.decodeFromString(json)
        } else {
            emptyList()
        }
    }

    suspend fun saveButtonLayout(context: Context, buttons: List<ButtonConfig>) {
        val json = Json.encodeToString(ListSerializer(ButtonConfig.serializer()), buttons)
        context.dataStore.edit { preferences ->
            preferences[BUTTON_LAYOUT_KEY] = json
        }
    }

    suspend fun saveHeartButtonLayout(context: Context, buttons: List<ButtonConfig>) {
        val json = Json.encodeToString(ListSerializer(ButtonConfig.serializer()), buttons)
        context.dataStore.edit { preferences ->
            preferences[HEART_BUTTON_LAYOUT_KEY] = json
        }
    }

    suspend fun loadButtonLayout(context: Context): List<ButtonConfig> {
        val preferences = context.dataStore.data.first()
        val json = preferences[BUTTON_LAYOUT_KEY]
        return if (json != null) {
            Json.decodeFromString(ListSerializer(ButtonConfig.serializer()), json)
        } else {
            // Default layout
            listOf(
                ButtonConfig("paste_star", "⭐ ⭐\nআর অপেক্ষা নয়! আপনার ভিডিওকে দ্রুত ভাইরাল করে হাজার বা লক্ষ মানুষের কাছে পৌঁছে দিন। আমাদের বিশেষ প্যাকেজ-এর মাধ্যমে আপনার TikTok প্রোফাইল রাতারাতি জনপ্রিয় হবে for you তে।\n\n🎈আমাদের সফলতার প্যাকেজগুলো :\n🚀 ১ দিনের বুস্ট: মাত্র ১৫০ টাকা  আপনার ভিডিওতে পাচ্ছেন ১,২৫০+ লাইক এবং ৩,৫০০+ থেকে ১৬,৬০০+ ভিউ। \n✨ ২ দিনের বুস্ট: মাত্র ৩০০ টাকা আপনার ভিডিওতে পাচ্ছেন ২,৫০০+ লাইক এবং ৬,৯০০+ থেকে ৩৩,৩০০+ ভিউ। \n💎৩ দিনের বুস্ট: মাত্র ৪৫০ টাকা  আপনার ভিডিওতে পাচ্ছেন ৩,৭৫০+ লাইক এবং ১০,৪০০+ থেকে ৪৯,৯০০+ ভিউ। \nআপনার সুযোগ হাতছাড়া করবেন না!", "⭐"),
                ButtonConfig("paste_money", "01855883948\n✅[বিকাশ/নগদ]✅\n\n🤗পার্সোনাল নাম্বার! \n💸 সেন্ড মানি করুন! \n📸 স্ক্রিনশট দিন! \n⬇️ লাস্ট ৪ সংখ্যা দিন! \n\n❌ফ্লাক্সিলোড দিলে পেমেন্ট বাতিল❌", "💵"),
                ButtonConfig("paste_one", "আপনি টাকা পাঠাবেন এবং ভিডিও লিংক দিবেন, বাকিটা আমাদের কাজ", "1️⃣"),
                ButtonConfig("paste_white_circle", "ভাই আমরা ওরকম না বিশ্বাস করতে পারেন, আমাদের অনেক কাস্টমার আজ পর্যন্ত কেউ এ কথা বলতে পারেনি যে আমরা কাউকে ঠকিয়েছি", "⚪"),
                ButtonConfig("paste_stop", "২০ থেকে ৩০ মিনিট পর শুরু হয়ে যাবে ২৪ ঘন্টা পর্যন্ত আসবে এর ভেতর সবকিছু এসে যাবে", "🛑")
            )
        }
    }

    suspend fun loadHeartButtonLayout(context: Context): List<ButtonConfig> {
        val preferences = context.dataStore.data.first()
        val json = preferences[HEART_BUTTON_LAYOUT_KEY]
        return if (json != null) {
            Json.decodeFromString(ListSerializer(ButtonConfig.serializer()), json)
        } else {
            // Modified layout for the heart clipboard
            listOf(
                ButtonConfig("paste_star", "⭐ ⭐\nআর অপেক্ষা নয়! আপনার ভিডিওকে দ্রুত ভাইরাল করে হাজার বা লক্ষ মানুষের কাছে পৌঁছে দিন। আমাদের বিশেষ প্যাকেজ-এর মাধ্যমে আপনার TikTok প্রোফাইল রাতারাতি জনপ্রিয় হবে for you তে 🥳।\n\n🚀 মাত্র ৩০০ টাকা  আপনার ভিডিওতে পাচ্ছেন ১,২৫০+ লাইক এবং ৩,৫০০+ থেকে ১৬,৬০০+ ভিউ। \n \nআমরা শুধু ভিউ এর গ্যারান্টি দেই, মানুষ লাইক ফলো শেয়ার কমেন্ট সবকিছু করবে ❤️ \n\nআপনার সুযোগ হাতছাড়া করবেন না!\n\nএর থেকেও ভালো প্যাকেজ আছে দেখবেন? 💰", "✨"),
                ButtonConfig("paste_sparkle_combo", "✨ মাত্র ৬০০ টাকা আপনার ভিডিওতে পাচ্ছেন ২,৫০০+ লাইক এবং ৬,৯০০+ থেকে ৩৩,৩০০+ ভিউ। \n💎 মাত্র ৯০০ টাকা  আপনার ভিডিওতে পাচ্ছেন ৩,৭৫০+ লাইক এবং ১০,৪০০+ থেকে ৪৯,৯০০+ ভিউ।\n\nঅথবা এর থেকে দামি প্যাকেজ দেখতে চাইলে আপনার বাজেট বলুন 🪙", "💫"),
                ButtonConfig("paste_money", "  01773675544\n✅[বিকাশ/নগদ]✅\n\n🤗পার্সোনাল নাম্বার! \n💸 সেন্ড মানি করুন! \n📸 স্ক্রিনশট দিন! \n⬇️ লাস্ট ৪ সংখ্যা দিন! \n\n❌ফ্লাক্সিলোড দিলে পেমেন্ট বাতিল❌", "💵"),
                ButtonConfig("paste_one", "আপনি টাকা পাঠাবেন এবং ভিডিও লিংক দিবেন, বাকিটা আমাদের কাজ", "➡️"),
                ButtonConfig("paste_white_circle", "ভাই আমরা ওরকম না বিশ্বাস করতে পারেন, আমাদের অনেক কাস্টমার আজ পর্যন্ত কেউ এ কথা বলতে পারেনি যে আমরা কাউকে ঠকিয়েছি", "⚪"),
                ButtonConfig("paste_stop", "২০ থেকে ৩০ মিনিট পর শুরু হয়ে যাবে ২৪ ঘন্টা পর্যন্ত আসবে এর ভেতর সবকিছু এসে যাবে", "🛑")
            )
        }
    }

    suspend fun save(context: Context, dataRows: List<DataRow>) {
        val json = Json.encodeToString(ListSerializer(DataRow.serializer()), dataRows)
        context.dataStore.edit {
            preferences ->
            preferences[DATA_ROWS_KEY] = json
        }
    }

    suspend fun load(context: Context): List<DataRow> {
        val preferences = context.dataStore.data.first()
        val json = preferences[DATA_ROWS_KEY]
        return if (json != null) {
            Json.decodeFromString(ListSerializer(DataRow.serializer()), json)
        } else {
            emptyList()
        }
    }

    suspend fun clearButtonLayout(context: Context) {
        context.dataStore.edit {
            preferences ->
            preferences.remove(BUTTON_LAYOUT_KEY)
        }
    }
}
