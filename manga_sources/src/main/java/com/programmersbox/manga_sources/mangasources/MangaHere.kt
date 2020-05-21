package com.programmersbox.manga_sources.mangasources

import com.squareup.duktape.Duktape
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

object MangaHere : MangaSource {

    private const val baseUrl = "https://www.mangahere.cc"

    override fun getManga(pageNumber: Int): List<MangaModel> = Jsoup.connect("$baseUrl/directory/$pageNumber.htm").get()
        .select(".manga-list-1-list li").map {
            MangaModel(
                title = it.select("a").first().attr("title"),
                description = "",
                mangaUrl = it.select("a").first().attr("abs:href"),
                imageUrl = it.select("img.manga-list-1-cover")?.first()?.attr("src") ?: "",
                source = Sources.MANGA_HERE
            )
        }.filter { it.title.isNotEmpty() }.sortedBy(MangaModel::title)

    override fun toInfoModel(model: MangaModel): MangaInfoModel {
        val doc = Jsoup.connect(model.mangaUrl).get()
        return MangaInfoModel(
            title = model.title,
            description = doc.select("p.fullcontent").text(),
            mangaUrl = model.mangaUrl,
            imageUrl = doc.select("img.detail-info-cover-img").select("img[src^=http]").attr("abs:src"),
            chapters = doc.select("div[id=chapterlist]").select("ul.detail-main-list").select("li").map {
                ChapterModel(
                    name = it.select("a").select("p.title3").text(),
                    url = it.select("a").attr("abs:href"),
                    uploaded = it.select("a").select("p.title2").text(),
                    sources = Sources.MANGA_HERE
                )
            },
            genres = doc.select("p.detail-info-right-tag-list").select("a").eachText(),
            alternativeNames = emptyList()
        )
    }

    override fun getPageInfo(chapterModel: ChapterModel): PageModel {
        return pageListParse(Jsoup.connect(chapterModel.url).get())
    }

    private fun pageListParse(document: Document): PageModel {
        val bar = document.select("script[src*=chapter_bar]")
        val duktape = Duktape.create()

        /*
            function to drop last imageUrl if it's broken/unneccesary, working imageUrls are incremental (e.g. t001, t002, etc); if the difference between
            the last two isn't 1 or doesn't have an Int at the end of the last imageUrl's filename, drop last Page
        */
        fun List<String>.dropLastIfBroken(): List<String> {
            val list = this.takeLast(2).map { page ->
                try {
                    page.substringBeforeLast(".").substringAfterLast("/").takeLast(2).toInt()
                } catch (_: NumberFormatException) {
                    return this.dropLast(1)
                }
            }
            return when {
                list[0] == 0 && 100 - list[1] == 1 -> this
                list[1] - list[0] == 1 -> this
                else -> this.dropLast(1)
            }
        }

        // if-branch is for webtoon reader, else is for page-by-page
        return PageModel(
            if (bar.isNotEmpty()) {
                val script = document.select("script:containsData(function(p,a,c,k,e,d))").html().removePrefix("eval")
                val deobfuscatedScript = duktape.evaluate(script).toString()
                val urls = deobfuscatedScript.substringAfter("newImgs=['").substringBefore("'];").split("','")
                duktape.close()

                urls.map { s -> "https:$s" }
            } else {
                val html = document.html()
                val link = document.location()

                var secretKey = extractSecretKey(html, duktape)

                val chapterIdStartLoc = html.indexOf("chapterid")
                val chapterId = html.substring(
                    chapterIdStartLoc + 11,
                    html.indexOf(";", chapterIdStartLoc)
                ).trim()

                val chapterPagesElement = document.select(".pager-list-left > span").first()
                val pagesLinksElements = chapterPagesElement.select("a")
                val pagesNumber = pagesLinksElements[pagesLinksElements.size - 2].attr("data-page").toInt()

                val pageBase = link.substring(0, link.lastIndexOf("/"))

                IntRange(1, pagesNumber).map { i ->

                    val pageLink = "$pageBase/chapterfun.ashx?cid=$chapterId&page=$i&key=$secretKey"

                    var responseText = ""

                    for (tr in 1..3) {

                        val request = Request.Builder()
                            .url(pageLink)
                            .addHeader("Referer", link)
                            .addHeader("Accept", "*/*")
                            .addHeader("Accept-Language", "en-US,en;q=0.9")
                            .addHeader("Connection", "keep-alive")
                            .addHeader("Host", "www.mangahere.cc")
                            .addHeader("User-Agent", System.getProperty("http.agent") ?: "")
                            .addHeader("X-Requested-With", "XMLHttpRequest")
                            .build()

                        val response = OkHttpClient().newCall(request).execute()
                        responseText = response.body?.string().toString()

                        if (responseText.isNotEmpty())
                            break
                        else
                            secretKey = ""
                    }

                    val deobfuscatedScript = duktape.evaluate(responseText.removePrefix("eval")).toString()

                    val baseLinkStartPos = deobfuscatedScript.indexOf("pix=") + 5
                    val baseLinkEndPos = deobfuscatedScript.indexOf(";", baseLinkStartPos) - 1
                    val baseLink = deobfuscatedScript.substring(baseLinkStartPos, baseLinkEndPos)

                    val imageLinkStartPos = deobfuscatedScript.indexOf("pvalue=") + 9
                    val imageLinkEndPos = deobfuscatedScript.indexOf("\"", imageLinkStartPos)
                    val imageLink = deobfuscatedScript.substring(imageLinkStartPos, imageLinkEndPos)

                    "https:$baseLink$imageLink"
                }
            }
                .dropLastIfBroken()
                .also { duktape.close() }
        )
    }

    private fun extractSecretKey(html: String, duktape: Duktape): String {

        val secretKeyScriptLocation = html.indexOf("eval(function(p,a,c,k,e,d)")
        val secretKeyScriptEndLocation = html.indexOf("</script>", secretKeyScriptLocation)
        val secretKeyScript = html.substring(secretKeyScriptLocation, secretKeyScriptEndLocation).removePrefix("eval")

        val secretKeyDeobfuscatedScript = duktape.evaluate(secretKeyScript).toString()

        val secretKeyStartLoc = secretKeyDeobfuscatedScript.indexOf("'")
        val secretKeyEndLoc = secretKeyDeobfuscatedScript.indexOf(";")

        val secretKeyResultScript = secretKeyDeobfuscatedScript.substring(
            secretKeyStartLoc, secretKeyEndLoc
        )

        return duktape.evaluate(secretKeyResultScript).toString()
    }

    override val hasMorePages: Boolean = true


}