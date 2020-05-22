package com.programmersbox.mangaworld.utils

import android.content.Context
import com.programmersbox.gsonutils.sharedPrefObjectDelegate
import com.programmersbox.helpfulutils.sharedPrefNotNullDelegate
import com.programmersbox.manga_db.MangaDbModel
import com.programmersbox.manga_sources.mangasources.MangaModel
import com.programmersbox.manga_sources.mangasources.Sources

var Context.usePalette: Boolean by sharedPrefNotNullDelegate(true)

var Context.currentSource: Sources? by sharedPrefObjectDelegate(defaultValue = Sources.values().random())

fun MangaDbModel.toMangaModel() = MangaModel(title, description, mangaUrl, imageUrl, source)
fun MangaModel.toMangaDbModel() = MangaDbModel(title, description, mangaUrl, imageUrl, source)