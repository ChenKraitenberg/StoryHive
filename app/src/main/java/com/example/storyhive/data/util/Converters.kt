package com.example.storyhive.data.local.util

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Room database converters for handling custom data types.
 * This class is used to convert complex data types to and from formats that can be stored in the database.
 */
class Converters {
    private val gson = Gson()

    //Converts a List<String> to a JSON string for database storage
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return gson.toJson(value)
    }

    //Converts a JSON string back to a List<String> when retrieving from the database
    @TypeConverter
    fun toStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return try {
            gson.fromJson(value, listType) ?: emptyList()
        } catch (e: Exception) {
            emptyList() // Returns an empty list in case of an error
        }
    }
}