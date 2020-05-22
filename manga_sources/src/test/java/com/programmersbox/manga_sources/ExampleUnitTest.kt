package com.programmersbox.manga_sources

import com.programmersbox.gsonutils.getApi
import com.programmersbox.manga_sources.mangasources.*
import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
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
        val d = f.random().toInfoModel()
        println(d)
        val s = d.chapters.random().getPageInfo()
        println(s)
    }

    @Test
    fun life() {
        val f = MangaFourLife.getManga()
        println(f.size)
        val d = f.find { it.title == "Dragon Ball" }!!.toInfoModel()
        println(d)
        val s = d.chapters.random().getPageInfo()
        println(s)
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

}