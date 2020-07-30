package com.programmersbox.manga_sources.mangasources

import android.content.Context
import androidx.lifecycle.ViewModel
import com.programmersbox.manga_sources.mangasources.utilities.NetworkHelper

interface MangaSource {
    val websiteUrl: String
    val hasMorePages: Boolean
    val headers: List<Pair<String, String>> get() = emptyList()
    fun getManga(pageNumber: Int = 1): List<MangaModel>
    fun toInfoModel(model: MangaModel): MangaInfoModel
    fun getMangaModelByUrl(url: String): MangaModel
    fun getPageInfo(chapterModel: ChapterModel): PageModel
    fun searchManga(searchText: CharSequence, pageNumber: Int = 1, mangaList: List<MangaModel>): List<MangaModel> =
        mangaList.filter { it.title.contains(searchText, true) }
}

data class MangaModel(
    val title: String,
    val description: String,
    val mangaUrl: String,
    val imageUrl: String,
    val source: Sources
) : ViewModel() {
    internal val extras = mutableMapOf<String, Any>()
    fun toInfoModel() = source.toInfoModel(this)
}

data class MangaInfoModel(
    val title: String,
    val description: String,
    val mangaUrl: String,
    val imageUrl: String,
    val chapters: List<ChapterModel>,
    val genres: List<String>,
    val alternativeNames: List<String>
) : ViewModel()

data class ChapterModel(
    val name: String,
    val url: String,
    val uploaded: String,
    val sources: Sources
) : ViewModel() {
    var uploadedTime: Long? = null
    fun getPageInfo() = sources.getPageInfo(this)
}

data class PageModel(val pages: List<String>)

object MangaContext {
    lateinit var context: Context

    @Volatile
    private var INSTANCE: NetworkHelper? = null

    fun getInstance(context: Context): NetworkHelper = INSTANCE ?: synchronized(this) { INSTANCE ?: NetworkHelper(context).also { INSTANCE = it } }
}
