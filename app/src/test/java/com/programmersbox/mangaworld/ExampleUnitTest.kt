package com.programmersbox.mangaworld

import com.programmersbox.gsonutils.fromJson
import com.programmersbox.gsonutils.toJson
import com.programmersbox.gsonutils.toPrettyJson
import com.programmersbox.helpfulutils.*
import com.programmersbox.manga_sources.mangasources.MangaModel
import com.programmersbox.manga_sources.mangasources.MangaSource
import com.programmersbox.manga_sources.mangasources.Sources
import com.programmersbox.manga_sources.mangasources.manga.MangaEden
import com.programmersbox.manga_sources.mangasources.manga.Manganelo
import org.junit.After
import org.junit.Test
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.*

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

    @After
    fun finished() {
        Runtime.getRuntime().exec("say finished").waitFor()
    }

    operator fun DateFormat.invoke(date: Any): String = format(date)

    @Test
    fun quickTimeTest() {
        val format = SimpleDateFormat("MM/dd/yyyy hh:mm:ss a")
        val f = nextTime(30.minutes.inMilliseconds.toLong())
        println(format.format(f))
        println(format(f))
        println(format(nextTime(1.5.hours.inMilliseconds.toLong())))
        println(format(nextTime(12.hours.inMilliseconds.toLong())))
        println(1.days.inMilliseconds.toLong())
        println(nextTimeInMs(1.days.inMilliseconds.toLong()))
        println(format(nextTime(1.days.inMilliseconds.toLong())))
        println(format(nextTime(3.days.inMilliseconds.toLong())))
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
        val d = g?.getManga()
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

    data class Student(val major: String, val firstName: String)

    class Classroom(val studentList: MutableList<Student>) {
        fun getStudents(): MutableList<Student> {
            return studentList
        }
    }

    @Test
    fun similarityTest() {

        val list = mutableListOf<Student>(
            Student("chemistry", "rafael"),
            Student("physics", "adam"),
            Student("chemistly", "michael"),
            Student("math", "jack"),
            Student("chemistry", "rafael"),
            Student("biology", "kevin"),
            Student("chemistly", "rafael")
        )

        val classroom = Classroom(list)
        val allStudents = classroom.getStudents()

        val finalList: MutableList<Pair<String, Classroom>> = mutableListOf()

        allStudents.map { it.major }.distinctBy { it }.forEach { major ->
            finalList.add(major to Classroom(allStudents.filter { s ->
                s.major.similarity(major).also { println("$s - $it") } >= .8f
            }.toMutableList()))
        }

        finalList.forEach {
            println(it.first + "->")
            it.second.getStudents().forEach { println("    " + it.major + ", " + it.firstName) }
        }

        println("-".repeat(50))

        val finalList2: MutableList<Pair<String, Classroom>> = mutableListOf()

        allStudents.forEach { name ->
            finalList2.add(name.firstName to Classroom(allStudents.filter { s ->
                s.major.similarity(name.major) >= .8f && s.firstName.similarity(name.firstName) >= .8f
            }.toMutableList()))
        }

        finalList2.distinctBy { it.first }.forEach {
            println(it.first + "->")
            it.second.getStudents().forEach { println("    " + it.major + ", " + it.firstName) }
        }

        val list3 = allStudents.groupBySimilarity({ it.firstName }) { s, name ->
            s.major.similarity(name.major) >= .8f && s.firstName.similarity(name.firstName) >= .8f
        }

        println("-".repeat(50))


        list3.forEach {
            println(it.first + "->")
            it.second.forEach { println("    " + it.major + ", " + it.firstName) }
        }

    }

    private fun <T, R> List<T>.groupBySimilarity(
        key: (T) -> R,
        predicate: (key: T, check: T) -> Boolean
    ): List<Pair<R, List<T>>> = map { name -> key(name) to filter { s -> predicate(name, s) } }.distinctBy { it.first }

    @Test
    fun timeTest() {
        val f = timeToNextHourOrHalf()
        println(f)
        val f1 = timeToNextHourOrHalfSdk()
        println(f1)
        val f2 = timeToNextHourOrHalfSdk2()
        println(f2)
    }

    private fun timeToNextHourOrHalf(): Long {
        val start = ZonedDateTime.now()
        // Hour + 1, set Minute and Second to 00
        val hour = start.plusHours(1).truncatedTo(ChronoUnit.HOURS)
        val minute = start.plusHours(0).truncatedTo(ChronoUnit.HOURS)
            .plusMinutes(30).truncatedTo(ChronoUnit.MINUTES).plusSeconds(1)

        // Get Duration
        val durationHour = Duration.between(start, hour).toMillis()
        val durationMinute = Duration.between(start, minute).toMillis()
        return if (durationHour <= durationMinute) durationHour else durationMinute
    }

    private fun timeToNextHourOrHalfSdk(): Long {
        val start = System.currentTimeMillis()

        val starter = Date(start)

        val hourer = Date(start).apply {
            hours += 1
            minutes = 0
            seconds = 0
        }.toInstant().truncatedTo(ChronoUnit.HOURS)
        val minuteer = Date(start).apply {
            minutes += 30
            seconds += 1
        }.toInstant()
            .plus(0, ChronoUnit.HOURS)
            .truncatedTo(ChronoUnit.HOURS)
            .plus(30, ChronoUnit.MINUTES)
            .truncatedTo(ChronoUnit.MINUTES)
            .plusSeconds(1)

        val durationHour = Duration.between(starter.toInstant(), hourer).toMillis()
        val durationMinute = Duration.between(starter.toInstant(), minuteer).toMillis()
        return if (durationHour <= durationMinute) durationHour else durationMinute
    }

    private fun timeToNextHourOrHalfSdk2(): Long {
        val start = System.currentTimeMillis()
        val hour = start + 3_600_000
        val minute = start + 1_800_000
        val hourer = Date(hour).apply {
            minutes += 0
            seconds += 0
        }
        val minuteer = Date(minute).apply {
            minutes += 30
            seconds += 1
        }

        println(hourer)
        println(minuteer)

        /*val durationHour = Duration.between(starter.toInstant(), hourer).toMillis()
        val durationMinute = Duration.between(starter.toInstant(), minuteer).toMillis()
        return if (durationHour <= durationMinute) durationHour else durationMinute*/

        return 0
    }

}
