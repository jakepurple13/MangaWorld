package com.programmersbox.mangaworld.utils

import android.content.Context
import com.programmersbox.gsonutils.getObject
import com.programmersbox.gsonutils.putObject
import com.programmersbox.gsonutils.sharedPrefNotNullObjectDelegate
import com.programmersbox.helpfulutils.defaultSharedPref
import com.programmersbox.helpfulutils.sharedPrefNotNullDelegate
import com.programmersbox.manga_db.MangaDbModel
import com.programmersbox.manga_sources.mangasources.MangaInfoModel
import com.programmersbox.manga_sources.mangasources.MangaModel
import com.programmersbox.manga_sources.mangasources.Sources


var Context.usePalette: Boolean by sharedPrefNotNullDelegate(true)
var Context.currentSource: Sources by sharedPrefNotNullObjectDelegate(defaultValue = Sources.values().random())
var Context.useCache: Boolean by sharedPrefNotNullDelegate(true)
var Context.cacheSize: Int by sharedPrefNotNullDelegate(5)
//var Context.mangaViewType: MangaListView by sharedPrefNotNullObjectDelegate(MangaListView.LINEAR)

object MangaInfoCache {
    private lateinit var context: Context
    fun init(context: Context) = run { this.context = context }

    fun getInfo() =
        if (context.useCache) context.defaultSharedPref.getObject<List<MangaInfoModel>>("mangaInfoCache", defaultValue = emptyList()) else null

    fun newInfo(mangaInfoModel: MangaInfoModel) {
        if (context.useCache) {
            val list = getInfo()!!.toMutableList()
            if (mangaInfoModel !in list) list.add(0, mangaInfoModel)
            if (list.size > context.cacheSize) list.removeAt(list.lastIndex)
            context.defaultSharedPref.edit().putObject("mangaInfoCache", list.distinctBy(MangaInfoModel::mangaUrl)).apply()
        }
    }
}

fun MangaDbModel.toMangaModel() = MangaModel(title, description, mangaUrl, imageUrl, source)
fun MangaModel.toMangaDbModel(numChapters: Int? = null) = MangaDbModel(title, description, mangaUrl, imageUrl, source)
    .apply { if (numChapters != null) this.numChapters = numChapters }

enum class MangaListView { LINEAR, GRID }
