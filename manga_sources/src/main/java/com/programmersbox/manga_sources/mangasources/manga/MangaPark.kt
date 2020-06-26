package com.programmersbox.manga_sources.mangasources.manga

import com.programmersbox.gsonutils.fromJson
import com.programmersbox.manga_sources.mangasources.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.util.*

object MangaPark : MangaSource {

    private const val baseUrl = "https://mangapark.net"

    override fun searchManga(searchText: CharSequence, pageNumber: Int, mangaList: List<MangaModel>): List<MangaModel> = try {
        if (searchText.isBlank()) throw Exception("No search necessary")
        Jsoup.connect("$baseUrl/search?q=$searchText&page=$pageNumber&st-ss=1").get()
            .select("div.item").map {
                val title = it.select("a.cover")
                MangaModel(
                    title = title.attr("title"),
                    description = it.select("p.summary").text(),
                    mangaUrl = "${baseUrl}${title.attr("href")}",
                    imageUrl = title.select("img").attr("abs:src"),
                    source = Sources.MANGA_PARK
                )
            }
    } catch (e: Exception) {
        super.searchManga(searchText, pageNumber, mangaList)
    }

    override fun getManga(pageNumber: Int): List<MangaModel> = Jsoup.connect("$baseUrl/latest/$pageNumber").get()
        .select("div.ls1").select("div.d-flex, div.flex-row, div.item")
        .map {
            MangaModel(
                title = it.select("a.cover").attr("title"),
                description = "",
                mangaUrl = "${baseUrl}${it.select("a.cover").attr("href")}",
                imageUrl = it.select("a.cover").select("img").attr("abs:src"),
                source = Sources.MANGA_PARK
            )
        }

    override fun toInfoModel(model: MangaModel): MangaInfoModel {
        val doc = Jsoup.connect(model.mangaUrl).get()
        val genres = mutableListOf<String>()
        val alternateNames = mutableListOf<String>()
        doc.select(".attr > tbody > tr").forEach {
            when (it.getElementsByTag("th").first().text().trim().toLowerCase(Locale.getDefault())) {
                "genre(s)" -> genres.addAll(it.getElementsByTag("a").map(Element::text))
                "alternative" -> alternateNames.addAll(it.text().split("l"))
            }
        }
        return MangaInfoModel(
            title = model.title,
            description = doc.select("p.summary").text(),
            mangaUrl = model.mangaUrl,
            imageUrl = model.imageUrl,
            chapters = doc.select("div.book-list-1").select("ul.chapter").select("li.d-flex, li.py-1, li.item ").map {
                val leftSide = it.select("div.tit").select("a")
                ChapterModel(
                    name = leftSide.text(),
                    url = leftSide.attr("abs:href").removeSuffix("/1"),
                    uploaded = it.select("span.time").text(),
                    sources = Sources.MANGA_PARK
                )
            },
            genres = genres,
            alternativeNames = alternateNames
        )
    }

    override fun getMangaModelByUrl(url: String): MangaModel {
        val doc = Jsoup.connect(url).get()
        val titleAndImg = doc.select("div.w-100, div.cover").select("img")
        return MangaModel(
            title = titleAndImg.attr("title"),
            description = doc.select("p.summary").text(),
            mangaUrl = url,
            imageUrl = titleAndImg.attr("abs:src"),
            source = Sources.MANGA_PARK
        )
    }

    override fun getPageInfo(chapterModel: ChapterModel): PageModel = PageModel(
        Jsoup.connect(chapterModel.url).get().toString()
            .substringAfter("var _load_pages = ").substringBefore(";").fromJson<List<Pages>>().orEmpty()
            .map { if (it.u.orEmpty().startsWith("//")) "https:${it.u}" else it.u.orEmpty() }
    )

    private data class Pages(val n: Number?, val w: String?, val h: String?, val u: String?)

    override val hasMorePages: Boolean = true
}