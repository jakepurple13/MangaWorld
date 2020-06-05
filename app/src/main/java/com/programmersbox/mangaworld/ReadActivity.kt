package com.programmersbox.mangaworld

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.programmersbox.gsonutils.getObjectExtra
import com.programmersbox.helpfulutils.defaultSharedPref
import com.programmersbox.helpfulutils.gone
import com.programmersbox.manga_sources.mangasources.ChapterModel
import com.programmersbox.mangaworld.adapters.PageAdapter
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_read.*

class ReadActivity : AppCompatActivity() {

    private val disposable = CompositeDisposable()
    private var model: ChapterModel? = null
    private val adapter = PageAdapter(this, mutableListOf())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_read)

        readView.adapter = adapter

        model = intent.getObjectExtra<ChapterModel>("currentChapter")

        Single.create<List<String>> { emitter ->
            try {
                emitter.onSuccess(model?.getPageInfo()?.pages.orEmpty())
            } catch (e: Exception) {
                emitter.onError(Throwable("Something went wrong. Please try again"))
            }
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnError { Toast.makeText(this, it.localizedMessage, Toast.LENGTH_SHORT).show() }
            .subscribe { pages: List<String> ->
                readLoading
                    .animate()
                    .alpha(0f)
                    .withEndAction { readLoading.gone() }
                    .start()
                adapter.addItems(pages)
                readView.layoutManager!!.scrollToPosition(model?.url?.let { defaultSharedPref.getInt(it, 0) } ?: 0)
            }
            .addTo(disposable)
    }

    private fun saveCurrentChapterSpot() {
        model?.let {
            defaultSharedPref.edit().apply {
                val currentItem = (readView.layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition()
                if (currentItem >= adapter.dataList.size - 2) remove(it.url)
                else putInt(it.url, currentItem)
            }.apply()
        }
    }

    override fun onPause() {
        saveCurrentChapterSpot()
        super.onPause()
    }

    override fun onDestroy() {
        saveCurrentChapterSpot()
        disposable.dispose()
        super.onDestroy()
    }
}
