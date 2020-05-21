package com.programmersbox.manga_sources.mangasources

import com.github.salomonbrys.kotson.get
import com.github.salomonbrys.kotson.string
import com.google.gson.JsonElement
import com.programmersbox.gsonutils.fromJson
import com.programmersbox.gsonutils.getJsonApi
import org.jsoup.Jsoup

object MangaFourLife : MangaSource {

    override fun getManga(pageNumber: Int): List<MangaModel> = getJsonApi<List<Life>>("https://manga4life.com/_search.php")?.map {
        MangaModel(
            title = it.s.toString(),
            description = "",
            mangaUrl = "https://manga4life.com/manga/${it.i}",
            imageUrl = "https://static.mangaboss.net/cover/${it.i}.jpg",
            source = Sources.MANGA_4_LIFE
        )
    }.orEmpty()

    override fun toInfoModel(model: MangaModel): MangaInfoModel {
        val doc = Jsoup.connect(model.mangaUrl).get()
        val description = doc.select("div.BoxBody > div.row").select("div.Content").text()
        val genres = "\"genre\":[^:]+(?=,|\$)".toRegex().find(doc.html())
            ?.groupValues?.get(0)?.removePrefix("\"genre\": ")?.fromJson<List<String>>().orEmpty()
        val altNames = "\"alternateName\":[^:]+(?=,|\$)".toRegex().find(doc.html())
            ?.groupValues?.get(0)?.removePrefix("\"alternateName\": ")?.fromJson<List<String>>().orEmpty()
        return MangaInfoModel(
            title = model.title,
            description = description,
            mangaUrl = model.mangaUrl,
            imageUrl = model.imageUrl,
            chapters = "vm.Chapters = (.*?);".toRegex().find(doc.html())
                ?.groupValues?.get(0)?.removePrefix("vm.Chapters = ")?.removeSuffix(";")?.fromJson<List<LifeChapter>>()?.map {
                    ChapterModel(
                        name = chapterImage(it.Chapter!!),
                        url = "https://manga4life.com/read-online/${model.mangaUrl.split("/").last()}${chapterURLEncode(it.Chapter)}",
                        uploaded = it.Date.toString(),
                        sources = Sources.MANGA_4_LIFE
                    )
                }.orEmpty(),
            genres = genres,
            alternativeNames = altNames
        )
    }

    private fun chapterURLEncode(e: String): String {
        var index = ""
        val t = e.substring(0, 1).toInt()
        if (1 != t) index = "-index-$t"
        val n = e.substring(1, e.length - 1)
        var suffix = ""
        val path = e.substring(e.length - 1).toInt()
        if (0 != path) suffix = ".$path"
        return "-chapter-$n$index$suffix.html"
    }

    private fun chapterImage(e: String): String {
        val a = e.substring(1, e.length - 1)
        val b = e.substring(e.length - 1).toInt()
        return if (b == 0) a else "$a.$b"
    }

    override fun getPageInfo(chapterModel: ChapterModel): PageModel {
        val document = Jsoup.connect(chapterModel.url).get()
        val script = document.select("script:containsData(MainFunction)").first().data()
        val curChapter = script.substringAfter("vm.CurChapter = ").substringBefore(";").fromJson<JsonElement>()!!

        val pageTotal = curChapter["Page"].string.toInt()

        val host = "https://" + script.substringAfter("vm.CurPathName = \"").substringBefore("\"")
        val titleURI = script.substringAfter("vm.IndexName = \"").substringBefore("\"")
        val seasonURI = curChapter["Directory"].string
            .let { if (it.isEmpty()) "" else "$it/" }
        val path = "$host/manga/$titleURI/$seasonURI"

        val chNum = chapterImage(curChapter["Chapter"].string)

        return PageModel(IntRange(1, pageTotal).mapIndexed { i, _ ->
            val imageNum = (i + 1).toString().let { "000$it" }.let { it.substring(it.length - 3) }
            "$path$chNum-$imageNum.png"
        })
    }

    override val hasMorePages: Boolean = false

    private data class Life(val i: String?, val s: String?, val a: List<String>?)

    private data class LifeChapter(val Chapter: String?, val Type: String?, val Date: String?, val ChapterName: String?)

}