package com.programmersbox.mangaworld

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.programmersbox.gsonutils.getObjectExtra
import com.programmersbox.manga_sources.mangasources.ChapterModel
import com.programmersbox.mangaworld.adapters.PageAdapter
import com.veinhorn.scrollgalleryview.MediaInfo
import com.veinhorn.scrollgalleryview.ScrollGalleryView
import com.veinhorn.scrollgalleryview.builder.GallerySettings
import com.veinhorn.scrollgalleryview.loader.GlideImageLoader
import kotlinx.android.synthetic.main.activity_read.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ReadActivity : AppCompatActivity() {

    private val adapter = PageAdapter(this@ReadActivity, mutableListOf())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_read)

        pageRV.adapter = adapter

        val g = ScrollGalleryView.from(galleryView)
            .settings(
                GallerySettings
                    .from(supportFragmentManager)
                    .thumbnailSize(100)
                    .enableZoom(true)
                    .build()
            )
            .onImageLongClickListener { println("Downloading $it...") }
            .build()
            .hideThumbnailsOnClick(true)
            .hideThumbnailsAfter(2500)

        GlobalScope.launch {
            val pages = intent.getObjectExtra<ChapterModel>("currentChapter")?.getPageInfo()?.pages.orEmpty()
                .map { MediaInfo.mediaLoader(GlideImageLoader(it)) }
            //GlideImageLoader(it)
            runOnUiThread {
                //adapter.addItems(pages)
                //testImage.load(pages[0])
                g.addMedia(pages)
            }
        }

    }
}
