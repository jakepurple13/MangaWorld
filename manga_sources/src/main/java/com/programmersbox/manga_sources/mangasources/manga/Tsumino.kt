package com.programmersbox.manga_sources.mangasources.manga

import com.programmersbox.gsonutils.getJsonApi
import com.programmersbox.manga_sources.mangasources.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

object Tsumino : MangaSource {

    private const val baseUrl = "https://www.tsumino.com"

    override val websiteUrl: String = baseUrl

    override fun getManga(pageNumber: Int): List<MangaModel> = getJsonApi<Base>("$baseUrl/Search/Operate/?PageNumber=$pageNumber&Sort=Newest")
        ?.data
        ?.map {
            MangaModel(
                title = it.entry?.title.toString(),
                description = "${it.entry?.duration}",
                mangaUrl = it.entry?.id.toString(),
                imageUrl = it.entry?.thumbnailUrl ?: it.entry?.thumbnailTemplateUrl ?: "",
                source = Sources.TSUMINO
            )
        }.orEmpty()

    override fun toInfoModel(model: MangaModel): MangaInfoModel {
        val doc = Jsoup.connect("$baseUrl/entry/${model.mangaUrl}").get()

        return MangaInfoModel(
            title = model.title,
            description = getDesc(doc),
            mangaUrl = "$baseUrl/entry/${model.mangaUrl}",
            imageUrl = model.imageUrl,
            chapters = listOf(
                ChapterModel(
                    url = model.mangaUrl,
                    name = doc.select("#Pages").text(),
                    uploaded = "",
                    sources = Sources.TSUMINO
                )
            ),
            genres = doc.select("#Tag a").eachText(),
            alternativeNames = emptyList()
        )
    }

    override fun getMangaModelByUrl(url: String): MangaModel {
        val doc = Jsoup.connect(url).get()
        return MangaModel(
            title = doc.select("div.book-title").text(),
            description = getDesc(doc),
            mangaUrl = url,
            imageUrl = doc.select("div.book-page-cover > img").select("img").attr("abs:src"),
            source = Sources.TSUMINO
        )
    }

    private fun getDesc(document: Document): String {
        val stringBuilder = StringBuilder()
        val parodies = document.select("#Parody a")
        val characters = document.select("#Character a")
        if (parodies.size > 0) {
            stringBuilder.append("Parodies: ")
            parodies.forEach {
                stringBuilder.append(it.text())
                if (it != parodies.last())
                    stringBuilder.append(", ")
            }
        }
        if (characters.size > 0) {
            stringBuilder.append("\n\n")
            stringBuilder.append("Characters: ")
            characters.forEach {
                stringBuilder.append(it.text())
                if (it != characters.last())
                    stringBuilder.append(", ")
            }
        }
        return stringBuilder.toString()
    }

    override fun getPageInfo(chapterModel: ChapterModel): PageModel =
        PageModel(
            chapterModel.name.toIntOrNull()?.let { 1..it }?.map {
                "https://content.tsumino.com/thumbs/${chapterModel.url}/$it"
            }.orEmpty()
        )

    override val hasMorePages: Boolean = true

    private data class Base(val pageNumber: Number?, val pageCount: Number?, val data: List<Data>?)

    private data class Data(val entry: Entry?, val impression: String?, val historyPage: Number?)

    private data class Entry(
        val id: Number?,
        val title: String?,
        val rating: Number?,
        val duration: Number?,
        val collectionPosition: Number?,
        val entryType: String?,
        val thumbnailUrl: String?,
        val thumbnailTemplateUrl: String?,
        val filledOpinion: String?
    )

}