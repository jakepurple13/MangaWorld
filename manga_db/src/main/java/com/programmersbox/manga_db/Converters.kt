package com.programmersbox.manga_db

import androidx.room.TypeConverter
import com.programmersbox.gsonutils.fromJson
import com.programmersbox.gsonutils.toJson
import com.programmersbox.manga_sources.mangasources.ChapterModel
import com.programmersbox.manga_sources.mangasources.Sources

class Converters {
    @TypeConverter
    fun fromChapterModel(value: List<ChapterModel>): String {
        return value.toJson()
    }

    @TypeConverter
    fun stringToChapterModel(value: String): List<ChapterModel>? {
        return value.fromJson<List<ChapterModel>>()
    }

    @TypeConverter
    fun fromList(value: List<String>): String {
        return value.toJson()
    }

    @TypeConverter
    fun toList(value: String): List<String>? {
        return value.fromJson<List<String>>()
    }

    @TypeConverter
    fun fromSource(value: Sources): String {
        return value.toJson()
    }

    @TypeConverter
    fun toSource(value: String): Sources? {
        return value.fromJson<Sources>()
    }
}