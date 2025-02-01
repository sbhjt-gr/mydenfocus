package com.gorai.myedenfocus.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.gorai.myedenfocus.domain.model.SessionMood

class Converters {
    @TypeConverter
    fun fromString(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromList(list: List<String>): String {
        return Gson().toJson(list)
    }

    @TypeConverter
    fun fromSessionMood(mood: SessionMood): String {
        return mood.name
    }

    @TypeConverter
    fun toSessionMood(value: String): SessionMood {
        return SessionMood.valueOf(value)
    }
} 