package com.programmersbox.manga_sources.mangasources.manga

import com.programmersbox.gsonutils.getJsonApi
import com.programmersbox.gsonutils.header
import com.programmersbox.manga_sources.mangasources.*
import okhttp3.Request

object Mangamutiny : MangaSource {

    private const val baseUrl = "https://api.mangamutiny.org"
    private const val mangaApiPath = "/v1/public/manga"
    private const val chapterApiPath = "/v1/public/chapter"

    override val headers: List<Pair<String, String>>
        get() = listOf(
            "Accept" to "application/json",
            "Origin" to "https://mangamutiny.org"
        )

    private val header: Request.Builder.() -> Unit = { header(*headers.toTypedArray()) }

    override fun searchManga(searchText: CharSequence, pageNumber: Int, mangaList: List<MangaModel>): List<MangaModel> = try {
        if (searchText.isBlank()) throw Exception("No search necessary")
        getJsonApi<Munity>(
            "$baseUrl$mangaApiPath?sort=-lastReleasedAt&limit=20&text=$searchText${if (pageNumber != 1) "&skip=${pageNumber * 20}" else ""}",
            header
        )
            ?.items?.map {
                MangaModel(
                    title = it.title.orEmpty(),
                    description = "",
                    mangaUrl = "$baseUrl$mangaApiPath/${it.slug}",
                    imageUrl = it.thumbnail.orEmpty(),
                    source = Sources.MANGAMUTINY
                )
            }
            .orEmpty()
    } catch (e: Exception) {
        super.searchManga(searchText, pageNumber, mangaList)
    }

    override fun getManga(pageNumber: Int): List<MangaModel> = getJsonApi<Munity>(
        "$baseUrl$mangaApiPath?sort=-lastReleasedAt&limit=20${if (pageNumber != 1) "&skip=${pageNumber * 20}" else ""}",
        header
    )
        ?.items
        ?.map {
            MangaModel(
                title = it.title.orEmpty(),
                description = "",
                mangaUrl = "$baseUrl$mangaApiPath/${it.slug}",
                imageUrl = it.thumbnail.orEmpty(),
                source = Sources.MANGAMUTINY
            )
        }.orEmpty()

    override fun toInfoModel(model: MangaModel): MangaInfoModel = getJsonApi<MangaInfoMunity>(model.mangaUrl, header).let {
        MangaInfoModel(
            title = model.title,
            description = it?.summary.orEmpty(),
            mangaUrl = model.mangaUrl,
            imageUrl = model.imageUrl,
            chapters = it?.chapters?.map { c ->
                ChapterModel(
                    name = chapterTitleBuilder(c),
                    url = "$baseUrl$chapterApiPath/${c.slug}",
                    uploaded = c.releasedAt.orEmpty(),
                    sources = Sources.MANGAMUTINY
                )
            }.orEmpty(),
            genres = it?.genres.orEmpty(),
            alternativeNames = listOf(it?.alias.orEmpty())
        )
    }

    private fun chapterTitleBuilder(rootNode: MunityChapters): String {
        val volume = rootNode.volume//volumegetNullable("volume")?.asInt

        val chapter = rootNode.chapter?.toInt()//getNullable("chapter")?.asInt

        val textTitle = rootNode.title//getNullable("title")?.asString

        val chapterTitle = StringBuilder()
        if (volume != null) chapterTitle.append("Vol. $volume")
        if (chapter != null) {
            if (volume != null) chapterTitle.append(" ")
            chapterTitle.append("Chapter $chapter")
        }
        if (textTitle != null && textTitle != "") {
            if (volume != null || chapter != null) chapterTitle.append(" ")
            chapterTitle.append(textTitle)
        }

        return chapterTitle.toString()
    }

    override fun getMangaModelByUrl(url: String): MangaModel = getJsonApi<MangaInfoMunity>(url, header).let {
        MangaModel(
            title = it?.title.orEmpty(),
            description = it?.summary.orEmpty(),
            mangaUrl = url,
            imageUrl = it?.thumbnail.orEmpty(),
            source = Sources.MANGAMUTINY
        )
    }

    override fun getPageInfo(chapterModel: ChapterModel): PageModel = PageModel(getJsonApi<MunityPage>(chapterModel.url, header)?.let {
        val chapterUrl = "${it.storage}/${it.manga}/${it.id}/"
        it.images?.map { i -> "$chapterUrl$i" }
    }.orEmpty())

    override val hasMorePages: Boolean = true

    private data class Munity(val items: List<MangaMunity>?, val total: Number?)
    private data class MangaMunity(val title: String?, val slug: String?, val thumbnail: String?, val id: String?)
    private data class MangaInfoMunity(
        val status: String?,
        val genres: List<String>?,
        val chapterCount: Number?,
        val viewCount: Number?,
        val rating: Number?,
        val ratingCount: Number?,
        val title: String?,
        val summary: String?,
        val authors: String?,
        val artists: String?,
        val slug: String?,
        val updatedAt: String?,
        val thumbnail: String?,
        val lastReleasedAt: String?,
        val category: String?,
        val alias: String?,
        val subCount: Number?,
        val commentCount: Number?,
        val chapters: List<MunityChapters>?,
        val id: String?
    )

    private data class MunityChapters(
        val viewCount: Number?,
        val title: String?,
        val volume: Int?,
        val chapter: Number?,
        val slug: String?,
        val releasedAt: String?,
        val id: String?
    )

    private data class MunityPage(
        val images: List<String>?,
        val storage: String?,
        val viewCount: Number?,
        val title: String?,
        val volume: Number?,
        val chapter: Number?,
        val manga: String?,
        val slug: String?,
        val releasedAt: String?,
        val order: Number?,
        val id: String?
    )

}