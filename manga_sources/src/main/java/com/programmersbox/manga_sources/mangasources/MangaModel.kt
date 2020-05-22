package com.programmersbox.manga_sources.mangasources

import androidx.lifecycle.ViewModel

interface MangaSource {
    fun getManga(pageNumber: Int = 1): List<MangaModel>
    fun toInfoModel(model: MangaModel): MangaInfoModel
    fun getPageInfo(chapterModel: ChapterModel): PageModel
    fun searchManga(searchText: CharSequence, pageNumber: Int = 1, mangaList: List<MangaModel>): List<MangaModel> =
        mangaList.filter { it.title.contains(searchText, true) }

    val hasMorePages: Boolean
}

data class MangaModel(
    val title: String,
    val description: String,
    val mangaUrl: String,
    val imageUrl: String,
    val source: Sources
) : ViewModel() {
    internal val extras = mutableMapOf<String, Any>()
    fun toInfoModel() = source.source().toInfoModel(this)
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

data class ChapterModel(val name: String, val url: String, val uploaded: String, private val sources: Sources) : ViewModel() {
    fun getPageInfo() = sources.source().getPageInfo(this)
}

data class PageModel(val pages: List<String>)
