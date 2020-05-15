package com.programmersbox.manga_sources.mangasources

interface MangaSource {
    fun getManga(pageNumber: Int = 1): List<MangaModel>
    fun toInfoModel(model: MangaModel): MangaInfoModel
    fun getPageInfo(chapterModel: ChapterModel): PageModel
}

data class MangaModel(
    val title: String,
    val description: String,
    val mangaUrl: String,
    val imageUrl: String,
    val source: Sources
) {
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
)

data class ChapterModel(val name: String, val url: String, val uploaded: String, private val sources: Sources) {
    fun getPageInfo() = sources.source().getPageInfo(this)
}

data class PageModel(val pages: List<String>)
