package com.programmersbox.manga_sources.mangasources

import org.jsoup.Jsoup
import org.jsoup.nodes.Document

object Manganelo : MangaSource {

    override val hasMorePages: Boolean = true

    //override fun searchManga(searchText: CharSequence, pageNumber: Int, mangaList: List<MangaModel>): List<MangaModel> =
    //TODO: Work on this
    //Jsoup.connect("https://m.manganelo.com/advanced_search?s=all&orby=az&page=$pageNumber&keyw=${searchText.replace(" ".toRegex(), "_")}").get()
    //.toMangaModel()

    override fun getManga(pageNumber: Int): List<MangaModel> =
        Jsoup.connect("https://m.manganelo.com/advanced_search?s=all&orby=newest&page=$pageNumber").get().toMangaModel()

    private fun Document.toMangaModel() = select("div.content-genres-item").map {
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

        return MangaInfoModel(
            title = model.title,
            description = doc.select("div.panel-story-info-description").text().removePrefix("Description :").trim(),
            mangaUrl = model.mangaUrl,
            imageUrl = model.imageUrl,
            chapters = doc.select("ul.row-content-chapter").select("li.a-h").map {
                ChapterModel(
                    name = it.select("a.chapter-name").text(),
                    url = it.select("a.chapter-name").attr("abs:href"),
                    uploaded = it.select("span.chapter-time").attr("title"),
                    sources = model.source
                )
            },
            genres = info.getOrNull(3)?.select("td.table-value")?.select("a")?.map { it.text() }.orEmpty(),
            alternativeNames = info.getOrNull(0)?.select("td.table-value")?.map { it.text() }.orEmpty()
        )
    }

    override fun getPageInfo(chapterModel: ChapterModel): PageModel = PageModel(
        Jsoup.connect(chapterModel.url).get()
            .select("div.container-chapter-reader").select("img")
            .map { it.select("img[src^=http]").attr("abs:src") }
    )

}