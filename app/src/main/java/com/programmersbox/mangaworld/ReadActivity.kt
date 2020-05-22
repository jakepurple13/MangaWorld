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

    private val adapter = PageAdapter(this@ReadActivity, mutableListOf())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_read)

        pageRV.adapter = adapter

        GlobalScope.launch {
            val pages = intent.getObjectExtra<ChapterModel>("currentChapter")?.getPageInfo()?.pages.orEmpty()
            runOnUiThread { adapter.addItems(pages) }
        }

    }
}
