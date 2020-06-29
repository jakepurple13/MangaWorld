package com.programmersbox.manga_sources.mangasources.manga

import android.annotation.SuppressLint
import com.programmersbox.gsonutils.fromJson
import com.programmersbox.manga_sources.mangasources.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
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
            chapters = chapterListParse(doc),
            genres = genres,
            alternativeNames = alternateNames
        )
    }

    private fun chapterListParse(response: Document): List<ChapterModel> {
        fun List<SChapter>.getMissingChapters(allChapters: List<SChapter>): List<SChapter> {
            val chapterNums = this.map { it.chapterNumber }
            return allChapters.filter { it.chapterNumber !in chapterNums }.distinctBy { it.chapterNumber }
        }

        fun List<SChapter>.filterOrAll(source: String): List<SChapter> {
            val chapters = this.filter { it.scanlator!!.contains(source) }
            return if (chapters.isNotEmpty()) {
                (chapters + chapters.getMissingChapters(this)).sortedByDescending { it.chapterNumber }
            } else {
                this
            }
        }

        val mangaBySource = response.select("div[id^=stream]")
            .map { sourceElement ->
                var lastNum = 0F
                val sourceName = sourceElement.select("i + span").text()

                sourceElement.select(".volume .chapter li")
                    .reversed() // so incrementing lastNum works
                    .map { chapterElement ->
                        chapterFromElement(chapterElement, sourceName, lastNum)
                            .also { lastNum = it.chapterNumber }
                    }
                    .distinctBy { it.chapterNumber } // there's even duplicate chapters within a source ( -.- )
            }

        val chapters = mangaBySource.maxBy { it.count() }!!
        return (chapters + chapters.getMissingChapters(mangaBySource.flatten())).sortedByDescending { it.chapterNumber }.map {
            ChapterModel(
                name = it.name,
                url = "${baseUrl}${it.url}",
                uploaded = it.originalDate,
                sources = Sources.MANGA_PARK
            ).apply { uploadedTime = it.dateUploaded }
        }
    }

    private class SChapter {
        var url: String = ""
        var name: String = ""
        var chapterNumber: Float = 0f
        var dateUploaded: Long? = null
        var originalDate: String = ""
        var scanlator: String? = null
    }

    private fun chapterFromElement(element: Element, source: String, lastNum: Float): SChapter {
        fun Float.incremented() = this + .00001F
        fun Float?.orIncrementLastNum() = if (this == null || this < lastNum) lastNum.incremented() else this

        return SChapter().apply {
            url = element.select(".tit > a").first().attr("href").replaceAfterLast("/", "")
            name = element.select(".tit > a").first().text()
            // Get the chapter number or create a unique one if it's not available
            chapterNumber = Regex("""\b\d+\.?\d?\b""").findAll(name)
                .toList()
                .map { it.value.toFloatOrNull() }
                .let { nums ->
                    when {
                        nums.count() == 1 -> nums[0].orIncrementLastNum()
                        nums.count() >= 2 -> nums[1].orIncrementLastNum()
                        else -> lastNum.incremented()
                    }
                }
            dateUploaded = element.select(".time").firstOrNull()?.text()?.trim()?.let { parseDate(it) }
            originalDate = element.select(".time").firstOrNull()?.text()?.trim().toString()
            scanlator = source
        }
    }


    private val dateFormat = SimpleDateFormat("MMM d, yyyy, HH:mm a", Locale.ENGLISH)
    private val dateFormatTimeOnly = SimpleDateFormat("HH:mm a", Locale.ENGLISH)

    @SuppressLint("DefaultLocale")
    private fun parseDate(date: String): Long? {
        val lcDate = date.toLowerCase()
        if (lcDate.endsWith("ago")) return parseRelativeDate(lcDate)

        // Handle 'yesterday' and 'today'
        var relativeDate: Calendar? = null
        if (lcDate.startsWith("yesterday")) {
            relativeDate = Calendar.getInstance()
            relativeDate.add(Calendar.DAY_OF_MONTH, -1) // yesterday
        } else if (lcDate.startsWith("today")) {
            relativeDate = Calendar.getInstance()
        }

        relativeDate?.let {
            // Since the date is not specified, it defaults to 1970!
            val time = dateFormatTimeOnly.parse(lcDate.substringAfter(' '))
            val cal = Calendar.getInstance()
            cal.time = time

            // Copy time to relative date
            it.set(Calendar.HOUR_OF_DAY, cal.get(Calendar.HOUR_OF_DAY))
            it.set(Calendar.MINUTE, cal.get(Calendar.MINUTE))
            return it.timeInMillis
        }

        return dateFormat.parse(lcDate)?.time
    }

    /**
     * Parses dates in this form:
     * `11 days ago`
     */
    private fun parseRelativeDate(date: String): Long? {
        val trimmedDate = date.split(" ")

        if (trimmedDate[2] != "ago") return null

        val number = when (trimmedDate[0]) {
            "a" -> 1
            else -> trimmedDate[0].toIntOrNull() ?: return null
        }
        val unit = trimmedDate[1].removeSuffix("s") // Remove 's' suffix

        val now = Calendar.getInstance()

        // Map English unit to Java unit
        val javaUnit = when (unit) {
            "year" -> Calendar.YEAR
            "month" -> Calendar.MONTH
            "week" -> Calendar.WEEK_OF_MONTH
            "day" -> Calendar.DAY_OF_MONTH
            "hour" -> Calendar.HOUR
            "minute" -> Calendar.MINUTE
            "second" -> Calendar.SECOND
            else -> return null
        }

        now.add(javaUnit, -number)

        return now.timeInMillis
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