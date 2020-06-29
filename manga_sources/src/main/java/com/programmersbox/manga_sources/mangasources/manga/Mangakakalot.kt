package com.programmersbox.manga_sources.mangasources.manga

import com.programmersbox.manga_sources.mangasources.*
import org.jsoup.Jsoup
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

object Mangakakalot : MangaSource {
    private const val url = "https://mangakakalot.com"

    override fun searchManga(searchText: CharSequence, pageNumber: Int, mangaList: List<MangaModel>): List<MangaModel> = try {
        if (searchText.isBlank()) throw Exception("No search necessary")
        Jsoup.connect("$url/search/story/${searchText.replace(" ".toRegex(), "_")}").get()
            .select("div.panel_story_list").select("div.story_item")
            .map {
                val mangaUrl = it.select("h3.story_name").select("a").select("a").attr("abs:href")
                MangaModel(
                    title = it.select("h3.story_name").select("a").text(),
                    description = "",
                    mangaUrl = mangaUrl,
                    imageUrl = it.select("img").attr("abs:src"),
                    source = if (mangaUrl.contains("manganelo")) Sources.MANGANELO else Sources.MANGAKAKALOT
                )
            }
    } catch (e: Exception) {
        super.searchManga(searchText, pageNumber, mangaList)
    }

    override fun getManga(pageNumber: Int): List<MangaModel> = Jsoup
        .connect("$url/manga_list?type=latest&category=all&state=all&page=$pageNumber").get()
        .select("div.list-truyen-item-wrap").map {
            val mangaUrl = it.select("h3").select("a").attr("abs:href")
            MangaModel(
                title = it.select("h3").select("a").text(),
                description = it.select("p").text(),
                mangaUrl = mangaUrl,
                imageUrl = it.select("a").select("img").attr("abs:src"),
                source = if (mangaUrl.contains("manganelo")) Sources.MANGANELO else Sources.MANGAKAKALOT
            )
        }

    override fun toInfoModel(model: MangaModel): MangaInfoModel {
        val doc = Jsoup.connect(model.mangaUrl).get()
        return MangaInfoModel(
            title = model.title,
            description = doc.select("div#noidungm").text(),
            mangaUrl = model.mangaUrl,
            imageUrl = model.imageUrl,
            chapters = doc.select("div.chapter-list").select("div.row").map {
                ChapterModel(
                    name = it.select("span").select("a").attr("title"),
                    url = it.select("span").select("a").attr("abs:href"),
                    uploaded = it.select("span").last().text(),
                    sources = model.source
                ).apply { uploadedTime = parseChapterDate(uploaded) }
            },
            genres = doc.select("li:contains(Genre)").select("a").eachText().map { it.removeSuffix(",") },
            alternativeNames = doc.select("h2.story-alternatives").text().removePrefix("Alternative :").split(";")
        )
    }

    private val dateformat: SimpleDateFormat = SimpleDateFormat("MMM-dd-yy", Locale.ENGLISH)

    private fun parseChapterDate(date: String): Long? {
        return if ("ago" in date) {
            val value = date.split(' ')[0].toIntOrNull()
            val cal = Calendar.getInstance()
            when {
                value != null && "min" in date -> cal.apply { add(Calendar.MINUTE, value * -1) }
                value != null && "hour" in date -> cal.apply { add(Calendar.HOUR_OF_DAY, value * -1) }
                value != null && "day" in date -> cal.apply { add(Calendar.DATE, value * -1) }
                else -> null
            }?.timeInMillis
        } else {
            try {
                dateformat.parse(date)
            } catch (e: ParseException) {
                null
            }?.time
        }
    }

    override fun getMangaModelByUrl(url: String): MangaModel {
        val doc = Jsoup.connect(url).get()
        return MangaModel(
            title = doc.select("ul.manga-info-text > li > h1").text(),
            description = doc.select("div#noidungm").text(),
            mangaUrl = url,
            imageUrl = doc.select("div.manga-info-pic").select("img").attr("abs:src"),
            source = Sources.MANGAKAKALOT
        )
    }

    override fun getPageInfo(chapterModel: ChapterModel): PageModel = PageModel(Jsoup.connect(chapterModel.url).get()
        .select("div.vung-doc").select("img")
        .map { it.attr("abs:src") }
    )

    override val hasMorePages: Boolean = true
}