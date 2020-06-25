package com.programmersbox.manga_sources

import com.programmersbox.gsonutils.fromJson
import com.programmersbox.gsonutils.getApi
import com.programmersbox.gsonutils.toPrettyJson
import com.programmersbox.manga_sources.mangasources.MangaModel
import com.programmersbox.manga_sources.mangasources.Sources
import com.programmersbox.manga_sources.mangasources.manga.*
import org.jsoup.Jsoup
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        val f = Sources.values().filterNot(Sources::filterOutOfUpdate)
        println(f)
    }

    @Test
    fun fox() {
        val f = MangaFox.getManga()
        println(f.size)
    }

    @Test
    fun here() {
        val f = MangaHere.getManga()
        println(f.size)
        println(f.first())
        val r = MangaHere.searchManga("Dragon", 1, f)
        println(r)
        val d = f.first().toInfoModel()
        println(d)
        val c = d.chapters.first().getPageInfo()
        println(c)
    }

    @Test
    fun town() {
        val f = Jsoup.connect("http://www.mangatown.com/").get()
        println(f)
    }

    @Test
    fun inkr() {
        val f = INKR.getManga()
        println(f.size)
        val r = INKR.searchManga("Flower", 1, f)
        println(r)
        val d = f.random().toInfoModel()
        println(d)
        val s = d.chapters.random().getPageInfo()
        println(s)
    }

    @Test
    fun life() {
        val f = MangaFourLife.getManga()
        println(f.size)
        val r = MangaFourLife.searchManga("solo", 1, f)
        println(r)
        /*val d = f.filter { it.title.contains("Dragon", true) }.random().toInfoModel()
        println(d)
        val s = d.chapters.random().getPageInfo()
        println(s)*/
        println(MangaFourLife.getMangaModelByUrl("https://manga4life.com/manga/Shouwa-Genroku-Rakugo-Shinjuu"))
    }

    @Test
    fun lifeThree() {
        val f = MangaFourLife.getManga(1)
        println(f.size)
        val f2 = MangaFourLife.getManga(2)
        println(f2.size)

        println(f)
        println(f2)
    }

    @Test
    fun lifeTwo() {
        val regex = "vm\\.Directory = (.*?);".toRegex()
        val f = regex.find(Jsoup.connect("https://manga4life.com/search/?sort=lt&desc=true").get().html())
        println(f?.groupValues?.get(1))
    }

    @Test
    fun other() {
        val f = getApi("https://mangarock.com//query/android500/info?oid=mrs-serie-200307933&Country=")
        println(f)
    }

    @Test
    fun dog() {
        //val f1 = getApi("https://mangadog.club/index/latestupdate/getUpdateResult?page=1")
        //println(f1)
        val f = MangaDog.getManga()
        println(f.size)
        val d = f.first().toInfoModel()
        println(d)
        val s = d.chapters.random().getPageInfo()
        println(s)
    }

    @Test
    fun nelo() {
        val f = Manganelo.getManga()
        println(f.size)
        val d = f.first().toInfoModel()
        println(d)
        val s = d.chapters.random().getPageInfo()
        println(s)

        println(Manganelo.getMangaModelByUrl("https://manganelo.com/manga/ev922898"))
    }

    @Test
    fun nineAnime() {
        val f = NineAnime.getManga()
        println(f.size)
        //println(f.joinToString("\n"))
        val d = f.first().toInfoModel()
        println(d)
        val s = d.chapters.random().getPageInfo()
        println(s)

        println(NineAnime.getMangaModelByUrl("https://www.nineanime.com/manga/MUSUKO_GA_KAWAIKUTE_SHIKATAGANAI_MAZOKU_NO_HAHAOYA.html?waring=1"))
    }

    @Test
    fun eden() {
        val f = MangaEden.getManga()
        println(f.size)
        val d = f.first().toInfoModel()
        println(d)
        val s = d.chapters.random().getPageInfo()
        println(s)
    }

    @Test
    fun alot() {
        val f = Mangakakalot.getManga()
        println(f.size)
        val d = f.random().toInfoModel()
        println(d)
        val s = d.chapters.random().getPageInfo()
        println(s)
        //val dragon = Mangakakalot.searchManga("Dragon", 1, f)
        //println(dragon.joinToString("\n"))
        println(Mangakakalot.getMangaModelByUrl("https://mangakakalot.com/read-lu6ua158504852918"))
    }

    @Test
    fun allTest() {
        /* val f = listOf(
             Sources.MANGA_HERE,
             Sources.MANGAKAKALOT,
             Sources.NINE_ANIME,
             Sources.MANGA_4_LIFE,
             Sources.MANGANELO
         ).map { it.getManga() }.flatten().distinctBy { it.source }*/

        println(System.currentTimeMillis())

        val f = jsonManga.fromJson<List<MangaModel>>()

        println(f)

        val f1 = f?.map { it.toInfoModel() }

        //println(f1.toPrettyJson())

        val f2 = f1?.map { it.chapters.random() }

        println(f2?.toPrettyJson())
    }

    private val jsonManga = """
        [
          {
            "extras": {},
            "title": "Talented Genius",
            "description": "",
            "mangaUrl": "https://www.mangahere.cc/manga/talented_genius/",
            "imageUrl": "http://fmcdn.mangahere.com/store/manga/32816/cover.jpg?token\u003db30b15d440f18c22e05fb0feea097400a027e745\u0026ttl\u003d1592935200\u0026v\u003d1592848827",
            "source": "MANGA_HERE",
            "mBagOfTags": {},
            "mCleared": false
          },
          {
            "extras": {},
            "title": "Torokeru Tsumugi-chan",
            "description": "1st Year High Schooler Tsumugi is a hard-working girl who can only think about studying. Even though she decided to devote her high school life to studying, the person who placed 1st in the entrance exam is a guy who has it all, Nishio Ryou, and she started seeing him as her rival. ... However!! Somehow being around Nishio Ryou just makes Tsumugi\u0027s heart race so much, she can\u0027 More.",
            "mangaUrl": "https://mangakakalot.com/manga/mq923315",
            "imageUrl": "https://mangadex.org/images/manga/32429.jpg?1592372566",
            "source": "MANGAKAKALOT",
            "mBagOfTags": {},
            "mCleared": false
          },
          {
            "extras": {},
            "title": "Kamiina Botan, Yoeru Sugata wa Yuri no Hana.",
            "description": "Kamiina Botan is a 20-year-old college student. At the welcome party for the dorm she was assigned to, the dorm leader Ibuki gives her a highball to drink and she becomes tipsy, leading to her involvement with Ibuki from now on... A tipsy teasing girl\u0027s comedy! More.",
            "mangaUrl": "https://manganelo.com/manga/ww923312",
            "imageUrl": "https://avt.mkklcdnv6.com/30/q/21-1592846056.jpg",
            "source": "MANGANELO",
            "mBagOfTags": {},
            "mCleared": false
          },
          {
            "extras": {},
            "title": "Matchless Emperor",
            "description": "",
            "mangaUrl": "https://www.nineanime.com/manga/Matchless_Emperor.html",
            "imageUrl": "https://img2.nineanime.com/files/img/logo/201912/201912311330301204.jpg",
            "source": "NINE_ANIME",
            "mBagOfTags": {},
            "mCleared": false
          },
          {
            "extras": {},
            "title": "The Last Saiyuki",
            "description": "Last updated: 2020-06-22T20:00:06+00:00",
            "mangaUrl": "https://manga4life.com/manga/The-Last-Saiyuki",
            "imageUrl": "https://static.mangaboss.net/cover/The-Last-Saiyuki.jpg",
            "source": "MANGA_4_LIFE",
            "mBagOfTags": {},
            "mCleared": false
          }
        ]
    """.trimIndent()

    @Test
    fun park() {

        val park = getApi("https://mangapark.net/latest")
        println(park)

        /*val f = MangaEden.getManga()
        println(f.size)
        val d = f.first().toInfoModel()
        println(d)
        val s = d.chapters.random().getPageInfo()
        println(s)*/
    }

}