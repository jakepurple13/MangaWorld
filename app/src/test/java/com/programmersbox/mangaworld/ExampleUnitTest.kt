package com.programmersbox.mangaworld

import com.programmersbox.mangaworld.mangasources.MangaSource
import com.programmersbox.mangaworld.mangasources.Manganelo
import org.junit.Test

import org.junit.Assert.*

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
}