package com.programmersbox.mangaworld

import androidx.core.app.TaskStackBuilder
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.programmersbox.gsonutils.toJson
import com.programmersbox.manga_sources.mangasources.MangaContext
import com.programmersbox.manga_sources.mangasources.Sources
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        //assertEquals("com.programmersbox.mangaworld", appContext.packageName)
        val f = TaskStackBuilder.create(appContext)
            .addParentStack(SettingsActivity::class.java)
            .toJson()
        println(f)
    }

    @Test
    fun timeout() = runBlocking {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        MangaContext.context = appContext

        val m = Sources.values().filterNot(Sources::isAdult)

        val m1 = m.map {
            it to withTimeoutOrNull(5000) {
                try {
                    it.getManga()
                } catch (e: Exception) {
                    null
                }
            }
        }

        m1.forEach {
            println(it)
        }

    }
}