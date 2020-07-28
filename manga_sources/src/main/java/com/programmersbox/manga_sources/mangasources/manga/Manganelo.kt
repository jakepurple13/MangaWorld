package com.programmersbox.manga_sources.mangasources.manga

import com.programmersbox.manga_sources.mangasources.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.text.SimpleDateFormat
import java.util.*

object Manganelo : MangaSource {

    override val hasMorePages: Boolean = true

    private const val baseUrl = "https://m.manganelo.com"
    override val websiteUrl: String = "https://manganelo.com"

    override fun searchManga(searchText: CharSequence, pageNumber: Int, mangaList: List<MangaModel>): List<MangaModel> = try {
        if (searchText.isBlank()) throw Exception("No search necessary")
        Jsoup.connect("$baseUrl/advanced_search?s=all&orby=latest&page=$pageNumber&keyw=$searchText").get().toMangaModel()
    } catch (e: Exception) {
        super.searchManga(searchText, pageNumber, mangaList)
    }

    override fun getManga(pageNumber: Int): List<MangaModel> =
        Jsoup.connect("$baseUrl/advanced_search?s=all&orby=latest&page=$pageNumber").get().toMangaModel()

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
                ).apply {
                    try {
                        uploadedTime = dateFormat.parse(uploaded)?.time
                    } catch (_: Exception) {
                    }
                }
            },
            genres = info.getOrNull(3)?.select("td.table-value")?.select("a")?.map { it.text() }.orEmpty(),
            alternativeNames = info.getOrNull(0)?.select("td.table-value")?.map { it.text() }.orEmpty()
        )
    }

    private val dateFormat = SimpleDateFormat("MMM dd,yyyy hh:mm", Locale.ENGLISH)

    override fun getMangaModelByUrl(url: String): MangaModel {
        val doc = Jsoup.connect(url).get()

        return MangaModel(
            title = doc.select("div.story-info-right").select("h1").text(),
            description = doc.select("div.panel-story-info-description").text().removePrefix("Description :").trim(),
            mangaUrl = url,
            imageUrl = doc.select("div.story-info-left").select("span.info-image").select("img").attr("abs:src"),
            source = Sources.MANGANELO
        )
    }

    override fun getPageInfo(chapterModel: ChapterModel): PageModel = PageModel(
        pages = Jsoup.connect(chapterModel.url).header("Referer", baseUrl).get()
            .select("div.container-chapter-reader").select("img")
            .map { it.select("img[src^=http]").attr("abs:src") }
    )

    override val headers: List<Pair<String, String>> = listOf(
        "Referer" to "https://manganelo.com"
    )

}