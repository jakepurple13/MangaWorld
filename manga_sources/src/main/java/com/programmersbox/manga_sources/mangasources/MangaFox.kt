package com.programmersbox.manga_sources.mangasources

import org.jsoup.Jsoup

object MangaFox : MangaSource {

    private const val url = "http://mangafox.me"

    override fun getManga(pageNumber: Int): List<MangaModel> {
        val doc = Jsoup.connect("$url/manga/").get()
        println(doc)
        return emptyList()
    }

    override fun toInfoModel(model: MangaModel): MangaInfoModel {
        return MangaInfoModel(
            title = model.title,
            description = model.description,
            mangaUrl = model.mangaUrl,
            imageUrl = model.imageUrl,
            chapters = emptyList(),
            genres = emptyList(),
            alternativeNames = emptyList()
        )
    }

    override fun getPageInfo(chapterModel: ChapterModel): PageModel {
        return PageModel(emptyList())
    }

    override val hasMorePages: Boolean = false

}