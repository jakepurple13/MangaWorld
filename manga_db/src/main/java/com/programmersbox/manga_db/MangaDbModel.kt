package com.programmersbox.manga_db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.programmersbox.manga_sources.mangasources.Sources

@Entity(tableName = "FavoriteManga")
data class MangaDbModel(
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "description")
    val description: String,
    @PrimaryKey
    @ColumnInfo(name = "mangaUrl")
    val mangaUrl: String,
    @ColumnInfo(name = "imageUrl")
    val imageUrl: String,
    @ColumnInfo(name = "sources")
    val source: Sources
)

@Entity(tableName = "ChaptersRead")
data class MangaReadChapter(
    @PrimaryKey
    @ColumnInfo(name = "url")
    val url: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "mangaUrl")
    val mangaUrl: String
)