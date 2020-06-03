package com.programmersbox.mangaworld

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.programmersbox.gsonutils.getObjectExtra
import com.programmersbox.manga_sources.mangasources.ChapterModel
import com.veinhorn.scrollgalleryview.MediaInfo
import com.veinhorn.scrollgalleryview.ScrollGalleryView
import com.veinhorn.scrollgalleryview.builder.GallerySettings
import com.veinhorn.scrollgalleryview.loader.GlideImageLoader
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_read.*

class ReadActivity : AppCompatActivity() {

    private val disposable = CompositeDisposable()

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

        Single.create<List<MediaInfo>> {
            try {
                it.onSuccess(intent.getObjectExtra<ChapterModel>("currentChapter")?.getPageInfo()?.pages.orEmpty()
                    .map { MediaInfo.mediaLoader(GlideImageLoader(it)) })
            } catch (e: Exception) {
                it.onError(Throwable("Something went wrong. Please try again"))
            }
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnError { Toast.makeText(this, it.localizedMessage, Toast.LENGTH_SHORT).show() }
            .subscribe { pages: List<MediaInfo> ->
                g.addMedia(pages)
                g.addOnImageLongClickListener { println("Downloading $it...") }
            }
            .addTo(disposable)

    }

    override fun onDestroy() {
        disposable.dispose()
        super.onDestroy()
    }
}
