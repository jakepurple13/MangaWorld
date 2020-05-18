package com.programmersbox.manga_sources.mangasources

import org.jsoup.Jsoup

object Manganelo : MangaSource {

    override val hasMorePages: Boolean = true

    override fun getManga(pageNumber: Int): List<MangaModel> =
        Jsoup.connect("https://m.manganelo.com/advanced_search?s=all&orby=az&page=$pageNumber").get()
            .select("div.content-genres-item").map {
                MangaModel(
                    title = it.select("a[href^=http]").attr("title"),
                    description = it.select("div.genres-item-description").text(),
                    mangaUrl = it.select("a[href^=http]").attr("abs:href"),
                    imageUrl = it.select("img").select("img[src^=http]").attr("abs:src"),
                    source = Sources.MANGANELO
                )
            }.filter { it.title.isNotEmpty() }

    override fun toInfoModel(model: MangaModel): MangaInfoModel {
        val doc = Jsoup.connect(model.mangaUrl).get()
        val info = doc.select("tbody").select("tr").select("td.table-value")
        val name = info[0].select("td.table-value").map { it.text() }
        val genre = info[3].select("td.table-value").select("a").map { it.text() }
        val chapters = doc.select("ul.row-content-chapter").select("li.a-h").map {
            ChapterModel(
                name = it.select("a.chapter-name").text(),
                url = it.select("a.chapter-name").attr("abs:href"),
                uploaded = it.select("span.chapter-time").attr("title"),
                sources = model.source
            )
        }

        return MangaInfoModel(
            title = model.title,
            description = model.description,
            mangaUrl = model.mangaUrl,
            imageUrl = model.imageUrl,
            chapters = chapters,
            genres = genre,
            alternativeNames = name
        )
    }

    override fun getPageInfo(chapterModel: ChapterModel): PageModel = PageModel(
        Jsoup.connect(chapterModel.url).get()
            .select("div.container-chapter-reader").select("img")
            .map { it.select("img[src^=http]").attr("abs:src") }
    )

}