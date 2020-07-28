package com.programmersbox.manga_sources.mangasources.manga

import android.annotation.SuppressLint
import com.programmersbox.gsonutils.getJsonApi
import com.programmersbox.manga_sources.mangasources.*
import org.jsoup.Jsoup
import java.text.SimpleDateFormat
import java.util.*

object MangaDog : MangaSource {

    private const val baseUrl = "https://mangadog.club"
    private const val cdn = "https://cdn.mangadog.club"

    override val websiteUrl: String = baseUrl

    override fun getManga(pageNumber: Int): List<MangaModel> = getJsonApi<Base>("$baseUrl/index/latestupdate/getUpdateResult?page=$pageNumber")
        ?.data
        ?.map {
            MangaModel(
                title = it.name.toString(),
                description = "Last Updated: ${it.last_update_time}",
                mangaUrl = "/detail/${it.search_name}/${it.id}.html",
                imageUrl = "$cdn${it.image?.replace("\\/", "/")}",
                source = Sources.INKR//MANGA_DOG
            ).apply { extras["comic_id"] = it.comic_id.toString() }
        }.orEmpty()

    @SuppressLint("DefaultLocale")
    override fun toInfoModel(model: MangaModel): MangaInfoModel {
        val doc = Jsoup.connect("$baseUrl/${model.mangaUrl}").get()

        return MangaInfoModel(
            title = model.title,
            description = doc.select("h2.fs15 + p").text().trim(),
            mangaUrl = "$baseUrl/${model.mangaUrl}",
            imageUrl = model.imageUrl,
            chapters = getJsonApi<BaseTwo>("$baseUrl/index/detail/getChapterList?comic_id=${model.extras["comic_id"]}&page=1")
                ?.data
                ?.data
                ?.map {
                    ChapterModel(
                        name = it.name.toString(),
                        url = "$baseUrl/read/read/${it.search_name}/${it.comic_id}.html",
                        uploaded = try {
                            SimpleDateFormat("MM/dd/yyyy hh:mm a", Locale.getDefault()).format(
                                SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(it.create_date.toString())?.time
                            )
                        } catch (e: Exception) {
                            it.create_date.toString()
                        },
                        sources = Sources.INKR//MANGA_DOG
                    )
                }
                .orEmpty(),
            genres = doc.select("div.col-sm-10.col-xs-9.text-left.toe.mlr0.text-left-m a[href*=genre]")
                .map { it.text().substringAfter(",").capitalize() },
            alternativeNames = emptyList()
        )
    }

    override fun getMangaModelByUrl(url: String): MangaModel = throw Exception("This doesn't work. I'll do this later")

    override fun getPageInfo(chapterModel: ChapterModel): PageModel =
        PageModel(
            pages = Jsoup.connect(chapterModel.url).get().body().select("img[data-src]").map { it.select("img").attr("data-src") }
        )

    override val hasMorePages: Boolean = true

    private data class Base(val data: List<DataTwo>?, val code: Number?, val msg: String?)

    private data class Chapters(val name: String?, val search_name: String?, val comic_id: Number?, val chapter_search_name: String?)

    private data class DataTwo(
        val id: Number?,
        val search_name: String?,
        val name: String?,
        val last_update_time: String?,
        val views: Number?,
        val image: String?,
        val main_subject: String?,
        val sub_subject: String?,
        val last_chapter_ids: String?,
        val subjects: List<Subjects>?,
        val chapters: List<Chapters>?,
        val comic_search_name: String?,
        val comic_id: Number?,
        val create_date: String?,
        val comic_name: String?
    )

    private data class Subjects(val name: String?, val search_name: String?)

    private data class BaseTwo(val data: DataThree?, val code: String?, val msg: String?)

    private data class DataThree(val pageNum: Number?, val data: List<DataFour>?)

    private data class DataFour(
        val create_date: String?,
        val name: String?,
        val id: Number?,
        val search_name: String?,
        val comic_id: Number?,
        val obj_id: String?
    )

}