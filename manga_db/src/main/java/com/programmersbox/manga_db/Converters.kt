package com.programmersbox.manga_db

import androidx.room.TypeConverter
import com.programmersbox.gsonutils.fromJson
import com.programmersbox.gsonutils.toJson
import com.programmersbox.manga_sources.mangasources.ChapterModel
import com.programmersbox.manga_sources.mangasources.Sources

class Converters {
    @TypeConverter
    fun fromChapterModel(value: List<ChapterModel>): String = value.toJson()

    @TypeConverter
    fun stringToChapterModel(value: String): List<ChapterModel>? = value.fromJson<List<ChapterModel>>()

    @TypeConverter
    fun fromList(value: List<String>): String = value.toJson()

    @TypeConverter
    fun toList(value: String): List<String>? = value.fromJson<List<String>>()

    @TypeConverter
    fun fromSource(value: Sources): String = value.toJson()

    @TypeConverter
    fun toSource(value: String): Sources? = value.fromJson<Sources>()
}