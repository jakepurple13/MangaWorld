package com.programmersbox.mangaworld.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.programmersbox.gsonutils.getObject
import com.programmersbox.gsonutils.getObjectExtra
import com.programmersbox.gsonutils.putObject
import com.programmersbox.gsonutils.sharedPrefObjectDelegate
import com.programmersbox.helpfulutils.defaultSharedPref
import com.programmersbox.helpfulutils.sharedPrefNotNullDelegate
import com.programmersbox.manga_db.MangaDbModel
import com.programmersbox.manga_sources.mangasources.MangaInfoModel
import com.programmersbox.manga_sources.mangasources.MangaModel
import com.programmersbox.manga_sources.mangasources.Sources
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

var Context.usePalette: Boolean by sharedPrefNotNullDelegate(true)
var Context.currentSource: Sources? by sharedPrefObjectDelegate(defaultValue = Sources.values().random())
var Context.useCache: Boolean by sharedPrefNotNullDelegate(true)

private var internalCacheSize: Int = 5
var Context.cacheSize
    get() = internalCacheSize
    set(value) = run { internalCacheSize = value }

object MangaInfoCache {
    private var context: Context? = null
    fun init(context: Context) = run { this.context = context }

    fun getInfo() =
        if (context!!.useCache) context!!.defaultSharedPref.getObject<List<MangaInfoModel>>("mangaInfoCache", defaultValue = emptyList()) else null

    fun newInfo(mangaInfoModel: MangaInfoModel) {
        if (context!!.useCache) {
            val list = getInfo()!!.toMutableList()
            if (mangaInfoModel !in list) list.add(0, mangaInfoModel)
            if (list.size > internalCacheSize) list.removeAt(list.lastIndex)
            context!!.defaultSharedPref.edit().putObject("mangaInfoCache", list.distinctBy(MangaInfoModel::mangaUrl)).apply()
        }
    }
}

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
