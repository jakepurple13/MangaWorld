package com.programmersbox.mangaworld

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.programmersbox.gsonutils.getObjectExtra
import com.programmersbox.manga_sources.mangasources.ChapterModel
import com.programmersbox.mangaworld.adapters.PageAdapter
import kotlinx.android.synthetic.main.activity_read.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class ReadActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_read)

        GlobalScope.launch {
            val pages = intent.getObjectExtra<ChapterModel>("chapter", null)?.getPageInfo()?.pages.orEmpty().toMutableList()
            runOnUiThread { pageRV.adapter = PageAdapter(this@ReadActivity, pages) }
        }

    }
}
