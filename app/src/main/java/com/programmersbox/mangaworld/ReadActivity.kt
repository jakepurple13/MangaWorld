package com.programmersbox.mangaworld

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.programmersbox.gsonutils.getObjectExtra
import com.programmersbox.manga_sources.mangasources.ChapterModel
import com.veinhorn.scrollgalleryview.MediaInfo
import com.veinhorn.scrollgalleryview.ScrollGalleryView
import com.veinhorn.scrollgalleryview.builder.GallerySettings
import com.veinhorn.scrollgalleryview.loader.GlideImageLoader
import kotlinx.android.synthetic.main.activity_read.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ReadActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_read)

        val g = ScrollGalleryView.from(galleryView)
            .settings(
                GallerySettings
                    .from(supportFragmentManager)
                    .thumbnailSize(100)
                    .enableZoom(true)
                    .build()
            )
            .build()
            .hideThumbnailsOnClick(true)
            .hideThumbnailsAfter(2500)

        GlobalScope.launch {
            val pages = intent.getObjectExtra<ChapterModel>("currentChapter")?.getPageInfo()?.pages.orEmpty()
                .map { MediaInfo.mediaLoader(GlideImageLoader(it)) }
            runOnUiThread {
                g.addMedia(pages)
                g.addOnImageLongClickListener { println("Downloading $it...") }
            }
        }

    }
}
