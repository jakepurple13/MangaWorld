package com.programmersbox.manga_sources.mangasources

import org.jsoup.Jsoup

object MangaHere : MangaSource {

    private val url = "http://www.mangahere.cc/mangalist/"

    override fun getManga(pageNumber: Int): List<MangaModel> = Jsoup.connect(url).get()
        .select("p.browse-new-block-content").map {
            MangaModel(
                title = it.select("a").attr("title"),
                description = "",
                mangaUrl = it.select("a").attr("abs:href"),
                imageUrl = "",
                source = Sources.MANGA_HERE
            )
        }.filter { it.title.isNotEmpty() }.sortedBy(MangaModel::title)

    override fun toInfoModel(model: MangaModel): MangaInfoModel {
        val doc = Jsoup.connect(model.mangaUrl).get()
        return MangaInfoModel(
            title = model.title,
            description = doc.select("p.fullcontent").text(),
            mangaUrl = model.mangaUrl,
            imageUrl = doc.select("img.detail-info-cover-img").select("img[src^=http]").attr("abs:src"),
            chapters = doc.select("div[id=chapterlist]").select("ul.detail-main-list").select("li").map {
                ChapterModel(
                    name = it.select("a").select("p.title3").text(),
                    url = it.select("a").attr("abs:href"),
                    uploaded = it.select("a").select("p.title2").text(),
                    sources = Sources.MANGA_HERE
                )
            },
            genres = doc.select("p.detail-info-right-tag-list").select("a").eachText(),
            alternativeNames = emptyList()
        )
    }

    override fun getPageInfo(chapterModel: ChapterModel): PageModel {
        val doc = Jsoup.connect(chapterModel.url).get()
        println(doc)
        return PageModel(emptyList())
    }

    override val hasMorePages: Boolean = false


}