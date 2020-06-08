package com.programmersbox.mangaworld

import com.programmersbox.gsonutils.fromJson
import com.programmersbox.gsonutils.toJson
import com.programmersbox.gsonutils.toPrettyJson
import com.programmersbox.helpfulutils.toHexString
import com.programmersbox.manga_sources.mangasources.MangaModel
import com.programmersbox.manga_sources.mangasources.MangaSource
import com.programmersbox.manga_sources.mangasources.Sources
import com.programmersbox.manga_sources.mangasources.manga.MangaEden
import com.programmersbox.manga_sources.mangasources.manga.Manganelo
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        val manganelo = Manganelo
        val list = manganelo.getManga()
        val list2 = manganelo.getManga()
        println(list.joinToString("\n"))
        println("-".repeat(100))
        println(list2.joinToString("\n"))
    }

    @Test
    fun tryTwo() {
        val manganelo = Manganelo
        //val list = manganelo.getManga()
        val list2 = manganelo.getManga()
        //println(list.joinToString("\n"))
        //println("-".repeat(100))
        //println(list2.joinToString("\n"))
        println(list2.random().toPrettyJson())
    }

    @Test
    fun tryThree() {
        val manganelo: MangaSource = Manganelo
        val list = manganelo.getManga()
        val f = list.first().toInfoModel()
        val f1 = f.chapters.first().getPageInfo()
        println(f1.pages.joinToString("\n"))
    }

    @Test
    fun mangaEdenTest() {
        val eden = MangaEden.getManga()
        val f = eden.random()
        println(f)
        val g = f.toInfoModel()
        println(g)
        //val d = g.chapters.first().getPageInfo()
        //println(d)
        //val s = d.pages.first()
        //println(s)
        //val doc = getApi("http://www.mangaeden.com/api/list/0/?p=1")
        //Jsoup.connect("http://www.mangaeden.com").get() /println(doc)
        //println(doc)
    }

    @Test
    fun similarTest() {
        val nelo = Manganelo.let { s -> (1..3).flatMap { s.getManga(it) } }
        val eden = MangaEden.getManga()

        val f = (nelo + eden).groupBy { it.title }.filter { it.value.size > 1 }

        println(f.entries.joinToString("\n") { "${it.key}=${it.value.map(MangaModel::source)}" })
    }

    @Test
    fun other() {
        objectTest()
        println("-".repeat(50))
        enumTest()
    }

    private fun objectTest() {
        println(MangaEden)
        val f = Manganelo.toJson()
        println(f)
        val g = f.fromJson<Manganelo>()
        println(g)
        val d = g?.getManga()
        println(d)
    }

    private fun enumTest() {
        println(Sources.MANGANELO)
        val f = Sources.MANGANELO.toJson()
        println(f)
        val g = f.fromJson<Sources>()
        println(g)
        val d = g?.source()?.getManga()
        println(d)
    }

    @Test
    fun colors() {
        /*listOf(
            0xfff44336,0xffe91e63,0xff9c27b0,0xff673ab7,
            0xff3f51b5,0xff2196f3,0xff03a9f4,0xff00bcd4,
            0xff009688,0xff4caf50,0xff8bc34a,0xffcddc39,
            0xffffeb3b,0xffffc107,0xffff9800,0xffff5722,
            0xff795548,0xff9e9e9e,0xff607d8b,0xff333333
        ).forEach {
            println(it.toHexString())
        }*/
        fun randomColor() = (Math.random() * 16777215).toInt() or (0xFF shl 24)
        for (i in 0..100) {
            println(randomColor().toHexString())
        }
    }

}