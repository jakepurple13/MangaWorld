package com.programmersbox.mangaworld.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.programmersbox.gsonutils.getObjectExtra
import com.programmersbox.gsonutils.sharedPrefObjectDelegate
import com.programmersbox.helpfulutils.sharedPrefNotNullDelegate
import com.programmersbox.manga_db.MangaDbModel
import com.programmersbox.manga_sources.mangasources.MangaModel
import com.programmersbox.manga_sources.mangasources.Sources
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

var Context.usePalette: Boolean by sharedPrefNotNullDelegate(true)
var Context.currentSource: Sources? by sharedPrefObjectDelegate(defaultValue = Sources.values().random())

fun MangaDbModel.toMangaModel() = MangaModel(title, description, mangaUrl, imageUrl, source)
fun MangaModel.toMangaDbModel() = MangaDbModel(title, description, mangaUrl, imageUrl, source)

inline fun <reified T> intentDelegate(
    key: String? = null,
    crossinline getter: Intent.(key: String, defaultValue: T?) -> T? = { k, d -> getObjectExtra(k, d) }
) = object : ReadOnlyProperty<Activity, T?> {
    private val keys: KProperty<*>.() -> String get() = { key ?: name }
    private var value: T? = null
    override operator fun getValue(thisRef: Activity, property: KProperty<*>): T? {
        if (value == null) value = thisRef.intent.getter(property.keys(), null)
        return value
    }
}
