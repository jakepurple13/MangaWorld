package com.programmersbox.mangaworld

import android.app.ProgressDialog
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

        //var chapter = intent.getIntExtra("chapter", 1)
        //val chapters = intent.getObjectExtra<List<ChapterModel>>("nextChapter", null)

        /*pageRV.addOnScrollListener(object : EndlessScrollingListener(pageRV.layoutManager!!) {
            override fun onLoadMore(page: Int, totalItemsCount: Int, view: RecyclerView?) {
                GlobalScope.launch {
                    chapter++
                    val pages = chapters?.getOrNull(chapter)?.getPageInfo()?.pages.orEmpty()
                    runOnUiThread { adapter.addItems(pages) }
                }
            }
        })*/

        val progress = ProgressDialog(this)
        progress.setTitle("Loading Pages...")
        progress.show()

        GlobalScope.launch {
            //val pages = chapters?.getOrNull(chapter)?.getPageInfo()?.pages.orEmpty()
            val pages = intent.getObjectExtra<ChapterModel>("currentChapter")?.getPageInfo()?.pages.orEmpty()
            runOnUiThread {
                adapter.addItems(pages)
                progress.hide()
            }
        }

    }
}
