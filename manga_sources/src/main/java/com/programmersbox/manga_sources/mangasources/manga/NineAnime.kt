package com.programmersbox.manga_sources.mangasources.manga

import com.programmersbox.manga_sources.mangasources.*
import org.jsoup.Jsoup
import java.net.URI
import java.net.URISyntaxException

object NineAnime : MangaSource {

    private const val url = "https://www.nineanime.com"

    override fun searchManga(searchText: CharSequence, pageNumber: Int, mangaList: List<MangaModel>): List<MangaModel> = try {
        if (searchText.isBlank()) throw Exception("No search necessary")
        Jsoup.connect("$url/search/?name=$searchText&page=$pageNumber.html").get()
            .select("div.post").map {
                MangaModel(
                    title = it.select("p.title a").text(),
                    description = "",
                    mangaUrl = it.select("p.title a").attr("href"),
                    imageUrl = it.select("img").attr("abs:src"),
                    source = Sources.NINE_ANIME
                )
            }
    } catch (e: Exception) {
        super.searchManga(searchText, pageNumber, mangaList)
    }

    override fun getManga(pageNumber: Int): List<MangaModel> = Jsoup.connect("$url/category/index_$pageNumber.html?sort=updated").get()
        .select("div.post").map {
            MangaModel(
                title = it.select("p.title a").text(),
                description = "",
                mangaUrl = it.select("p.title a").attr("href"),
                imageUrl = it.select("img").attr("abs:src"),
                source = Sources.NINE_ANIME
            )
        }

    override fun toInfoModel(model: MangaModel): MangaInfoModel {
        val doc = Jsoup.connect("${model.mangaUrl}?waring=1").get()
        val genreAndDescription = doc.select("div.manga-detailmiddle")
        return MangaInfoModel(
            title = model.title,
            description = genreAndDescription.select("p.mobile-none").text(),
            mangaUrl = model.mangaUrl,
            imageUrl = model.imageUrl,
            chapters = doc.select("ul.detail-chlist li").map {
                ChapterModel(
                    name = it.select("a").select("span").firstOrNull()?.text() ?: it.text() ?: it.select("a").text(),
                    url = it.select("a").attr("abs:href"),
                    uploaded = it.select("span.time").text(),
                    sources = Sources.NINE_ANIME
                )
            },
            genres = genreAndDescription.select("p:has(span:contains(Genre)) a").map { it.text() },
            alternativeNames = doc.select("div.detail-info").select("p:has(span:contains(Alternative))").text()
                .removePrefix("Alternative(s):").split(";")
        )
    }

    override fun getMangaModelByUrl(url: String): MangaModel {
        val doc = Jsoup.connect(url).get()
        val genreAndDescription = doc.select("div.manga-detailmiddle")
        return MangaModel(
            title = doc.select("div.manga-detail > h1").select("h1").text(),
            description = genreAndDescription.select("p.mobile-none").text(),
            mangaUrl = url,
            imageUrl = doc.select("img.detail-cover").attr("abs:src"),
            source = Sources.NINE_ANIME
        )
    }

    override fun getPageInfo(chapterModel: ChapterModel): PageModel {
        val doc = Jsoup.connect(chapterModel.url).header("Referer", "$url/manga.").get()
        val script = doc.select("script:containsData(all_imgs_url)").firstOrNull()?.data() ?: return PageModel(emptyList())
        return PageModel(Regex(""""(http.*)",""").findAll(script).map { it.groupValues[1] }.toList())
    }

    override val hasMorePages: Boolean = true

    private fun getUrlWithoutDomain(orig: String): String {
        return try {
            val uri = URI(orig)
            var out = uri.path
            if (uri.query != null) {
                out += "?" + uri.query
            }
            if (uri.fragment != null) {
                out += "#" + uri.fragment
            }
            out
        } catch (e: URISyntaxException) {
            orig
        }
    }
}