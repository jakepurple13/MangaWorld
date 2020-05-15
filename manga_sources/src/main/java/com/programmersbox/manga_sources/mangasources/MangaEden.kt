package com.programmersbox.manga_sources.mangasources

import com.programmersbox.gsonutils.getJsonApi
import java.text.SimpleDateFormat
import java.util.*

object MangaEden : MangaSource() {

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

    private const val baseUrl = "http://www.mangaeden.com"
    private const val imageUrl = "http://cdn.mangaeden.com/mangasimg/"

    override fun getManga(pageNumber: Int): List<MangaModel> = getJsonApi<Eden>("$baseUrl/api/list/0/")?.manga?.map {
        MangaModel(
            title = it.t ?: "",
            description = "",
            mangaUrl = "$baseUrl/api/manga/${it.i}/",
            imageUrl = "$imageUrl${it.im}",
            source = Sources.MANGA_EDEN
        )
    } ?: emptyList()

    override fun toInfoModel(model: MangaModel): MangaInfoModel {
        val details = getJsonApi<MangaDetails>(model.mangaUrl)
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
                )
            } ?: emptyList(),
            genres = details?.categories ?: emptyList(),
            alternativeNames = details?.aka ?: emptyList()
        )
    }

    override fun getPageInfo(chapterModel: ChapterModel): PageModel = PageModel(
        pages = getJsonApi<Pages>(chapterModel.url)?.images?.map { "$imageUrl/${it[1]}" } ?: emptyList()
    )

}