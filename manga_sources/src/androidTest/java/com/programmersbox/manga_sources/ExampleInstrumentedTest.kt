package com.programmersbox.manga_sources

import android.net.Uri
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.programmersbox.gsonutils.getApi
import com.programmersbox.gsonutils.header
import com.programmersbox.manga_sources.mangasources.manga.INKR
import org.junit.Assert.assertEquals
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
        assertEquals("com.programmersbox.manga_sources.test", appContext.packageName)
    }

    @Test
    fun inkr() {
        val f = INKR.getManga()
        println(f.size)
        val r = INKR.searchManga("Breath", 1, f)
        println(r)
        val d = f.random().toInfoModel()
        println(d)
        val s = d.chapters.random().getPageInfo()
        println(s)
    }

    @Test
    fun munity() {
        val baseUrl = "https://api.mangamutiny.org/"
        val apiPath = "v1/public/manga"
        val f = Uri.parse(baseUrl).buildUpon()
            .appendEncodedPath(apiPath)
            .appendQueryParameter("sort", "-rating -ratingCount")
            .appendQueryParameter("limit", "20")
            .build().toString()
        Log.println(Log.ASSERT, "tag", f)
        val api = getApi(f) {
            header(
                "Accept" to "application/json",
                "Origin" to "https://mangamutiny.org"
            )
        }

        println(api)
    }
}