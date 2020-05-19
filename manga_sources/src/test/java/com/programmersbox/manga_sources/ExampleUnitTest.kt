package com.programmersbox.manga_sources

import com.programmersbox.manga_sources.mangasources.MangaFox
import com.programmersbox.manga_sources.mangasources.MangaHere
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

}