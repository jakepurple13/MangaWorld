package com.programmersbox.mangaworld

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.bumptech.glide.util.ViewPreloadSizeProvider
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.googlematerial.GoogleMaterial
import com.mikepenz.iconics.utils.colorInt
import com.mikepenz.iconics.utils.sizePx
import com.programmersbox.gsonutils.getObjectExtra
import com.programmersbox.helpfulutils.*
import com.programmersbox.manga_sources.mangasources.ChapterModel
import com.programmersbox.mangaworld.adapters.PageAdapter
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_read.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

class ReadActivity : AppCompatActivity() {

    private val disposable = CompositeDisposable()
    private var model: ChapterModel? = null
    private val loader by lazy { Glide.with(this) }
    private val adapter by lazy {
        loader.let {
            PageAdapter(
                fullRequest = it
                    .asDrawable()
                    .centerCrop(),
                thumbRequest = it
                    .asDrawable()
                    .diskCacheStrategy(DiskCacheStrategy.DATA)
                    .transition(withCrossFade()),
                context = this@ReadActivity,
                dataList = mutableListOf()
            )
        }
    }

    private var batteryInfo: BroadcastReceiver? = null
    private var timeTicker: BroadcastReceiver? = null

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_read)

        val preloader: RecyclerViewPreloader<String> = RecyclerViewPreloader(loader, adapter, ViewPreloadSizeProvider(), 10)
        readView.addOnScrollListener(preloader)
        readView.setItemViewCacheSize(0)

        batteryInformation.startDrawable = IconicsDrawable(this, GoogleMaterial.Icon.gmd_battery_std).apply {
            colorInt = Color.WHITE
            sizePx = batteryInformation.textSize.roundToInt()
        }

        batteryInfo = battery { batteryInformation.text = "${it.percent.toInt()}%" }
        timeTicker = timeTick { _, _ -> currentTime.text = SimpleDateFormat("HH:mm a", Locale.getDefault()).format(System.currentTimeMillis()) }

        /*var range = ConstraintRange(
            readLayout,
            ConstraintSet().apply { clone(readLayout) },
            ConstraintSet().apply { clone(this@ReadActivity, R.layout.actvity_read_alt) }
        )

        GlobalScope.launch {
            runOnUiThread { range.current = 0 }
            delay(5000)
            runOnUiThread { range.current = 1 }
        }*/

        readView.adapter = adapter

        readView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val l = recyclerView.layoutManager as LinearLayoutManager
                val image = l.findLastVisibleItemPosition()
                if (image > -1) {
                    val total = l.itemCount
                    pageCount.text = String.format("%d/%d", image + 1, total)
                }
            }
        })

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
        unregisterReceiver(batteryInfo)
        unregisterReceiver(timeTicker)
        saveCurrentChapterSpot()
        disposable.dispose()
        super.onDestroy()
    }
}
