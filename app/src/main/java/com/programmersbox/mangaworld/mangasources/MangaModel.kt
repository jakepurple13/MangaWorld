package com.programmersbox.mangaworld.mangasources

abstract class MangaSource {
    var pageNumber: Int = 1
    abstract fun getManga(): List<MangaModel>
    abstract fun toInfoModel(model: MangaModel): MangaInfoModel
    abstract fun getPageInfo(chapterModel: ChapterModel): PageModel
}

data class MangaModel(
    val title: String,
    val description: String,
    val mangaUrl: String,
    val imageUrl: String,
    private val mangaSource: MangaSource
) {
    fun toInfoModel() = mangaSource.toInfoModel(this)
}

data class MangaInfoModel(
    val title: String,
    val description: String,
    val mangaUrl: String,
    val imageUrl: String,
    val chapters: List<ChapterModel>,
    val genres: GenreModel,
    val alternativeNames: List<String>
)

data class GenreModel(val genres: List<String>)

data class ChapterModel(val name: String, val url: String, val uploaded: String, private val mangaSource: MangaSource) {
    fun getPageInfo() = mangaSource.getPageInfo(this)
}

data class PageModel(val pages: List<String>)
