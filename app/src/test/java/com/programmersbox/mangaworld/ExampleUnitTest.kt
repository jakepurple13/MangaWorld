package com.programmersbox.mangaworld

import com.programmersbox.helpfulutils.intersect
import com.programmersbox.mangaworld.mangasources.MangaEden
import com.programmersbox.mangaworld.mangasources.MangaSource
import com.programmersbox.mangaworld.mangasources.Manganelo
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        val manganelo = Manganelo()
        val list = manganelo.getManga()
        manganelo.pageNumber++
        val list2 = manganelo.getManga()
        println(list.joinToString("\n"))
        println("-".repeat(100))
        println(list2.joinToString("\n"))
    }

    @Test
    fun tryTwo() {
        val manganelo = Manganelo()
        //val list = manganelo.getManga()
        manganelo.pageNumber++
        val list2 = manganelo.getManga()
        //println(list.joinToString("\n"))
        //println("-".repeat(100))
        println(list2.joinToString("\n"))
    }

    @Test
    fun tryThree() {
        val manganelo: MangaSource = Manganelo()
        val list = manganelo.getManga()
        val f = list.first().toInfoModel()
        val f1 = f.chapters.first().getPageInfo()
        println(f1.pages.joinToString("\n"))
    }

    @Test
    fun mangaEdenTest() {
        val eden = MangaEden().getManga()
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
        val nelo = Manganelo()
        val eden = MangaEden()

        val f = eden.getManga().intersect(nelo.getManga()) { u, o -> u.title == o.title }

        println(f.joinToString("\n"))
    }
}