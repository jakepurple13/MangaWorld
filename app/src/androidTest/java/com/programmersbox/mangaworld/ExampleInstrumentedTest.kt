package com.programmersbox.mangaworld

import androidx.core.app.TaskStackBuilder
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.programmersbox.gsonutils.toJson
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
}