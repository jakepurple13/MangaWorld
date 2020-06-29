package com.programmersbox.manga_sources.mangasources.manga

import com.programmersbox.gsonutils.getJsonApi
import com.programmersbox.manga_sources.mangasources.*
import com.programmersbox.manga_sources.mangasources.utilities.header
import okhttp3.Request
import java.text.SimpleDateFormat
import java.util.*

object MangaEden : MangaSource {

    private const val baseUrl = "http://www.mangaeden.com"
    private const val imageUrl = "http://cdn.mangaeden.com/mangasimg/"

    override val hasMorePages: Boolean = false

    override fun getManga(pageNumber: Int): List<MangaModel> = getJsonApi<Eden?>("$baseUrl/api/list/0/", header)?.manga
        ?.sortedByDescending { m -> m.ld?.let { 1000 * it.toDouble() } }
        ?.mapNotNull {
            if (it.ld == null || it.t.isNullOrEmpty()) null
            else MangaModel(
                title = it.t,
                description = "Last updated: ${SimpleDateFormat("MM/dd/yyyy hh:mm a", Locale.getDefault()).format(1000 * it.ld.toDouble())}",
                mangaUrl = "$baseUrl/api/manga/${it.i}/",
                imageUrl = "$imageUrl${it.im}",
                source = Sources.TSUMINO//Sources.MANGA_EDEN
            )
        }.orEmpty()

    override fun toInfoModel(model: MangaModel): MangaInfoModel {
        val details = getJsonApi<MangaDetails>(model.mangaUrl, header)
        return MangaInfoModel(
            title = model.title,
            description = details?.description ?: model.description,
            mangaUrl = model.mangaUrl,
            imageUrl = model.imageUrl,
            chapters = details?.chapters?.mapIndexed { index, list ->
                ChapterModel(
                    name = try {
                        list[2].toString()
                    } catch (e: NullPointerException) {
                        "$index"
                    },
                    url = "$baseUrl/api/chapter/${list[3]}/",
                    uploaded = SimpleDateFormat("MM/dd/yyyy hh:mm a", Locale.getDefault()).format(1000 * list[1] as Double),
                    sources = model.source
                ).apply { uploadedTime = (1000 * list[1] as Double).toLong() }
            } ?: emptyList(),
            genres = details?.categories ?: emptyList(),
            alternativeNames = details?.aka ?: emptyList()
        )
    }

    override fun getMangaModelByUrl(url: String): MangaModel {
        val details = getJsonApi<MangaDetails>(url, header)
        return MangaModel(
            title = details?.title.orEmpty(),
            description = details?.description.orEmpty(),
            mangaUrl = url,
            imageUrl = details?.imageURL.orEmpty(),
            source = Sources.TSUMINO//Sources.MANGA_EDEN
        )
    }

    override fun getPageInfo(chapterModel: ChapterModel): PageModel = PageModel(
        pages = getJsonApi<Pages>(chapterModel.url, header)?.images?.map { "$imageUrl${it[1]}" } ?: emptyList()
    )

    override val headers: List<Pair<String, String>>
        get() = listOf(
            "authority" to "www.mangaeden.com",
            "cache-control" to "max-age=0",
            "upgrade-insecure-requests" to "1",
            "user-agent" to "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.61 Safari/537.36",
            "accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9",
            "sec-fetch-site" to "none",
            "sec-fetch-mode" to "navigate",
            "sec-fetch-user" to "?1",
            "sec-fetch-dest" to "document",
            "accept-language" to "en-US,en;q=0.9"
        )

    private val header: Request.Builder.() -> Unit = { header(*headers.toTypedArray()) }

    private data class Eden(val end: Number?, val manga: List<Manga>?, val page: Number?, val start: Number?, val total: Number?)

    private data class Manga(
        val a: String?,
        val c: List<String>?,
        val h: Number?,
        val i: String?,
        val im: String?,
        val ld: Number?,
        val s: Number?,
        val t: String?
    )

    private data class MangaDetails(
        val aka: List<String>?,
        val `aka-alias`: List<String>?,
        val alias: String?,
        val artist: String?,
        val artist_kw: List<String>?,
        val author: String?,
        val author_kw: List<String>?,
        val baka: Boolean?,
        val categories: List<String>?,
        val chapters: List<List<Any>>?,
        val chapters_len: Number?,
        val created: Number?,
        val description: String?,
        val hits: Number?,
        val image: String?,
        val imageURL: String?,
        val language: Number?,
        val last_chapter_date: Number?,
        val random: Number?,
        val released: Number?,
        val startsWith: String?,
        val status: Number?,
        val title: String?,
        val title_kw: List<String>?,
        val type: Number?,
        val updatedKeywords: Boolean?
    )

    private data class Pages(val images: List<List<Any>>?)

}