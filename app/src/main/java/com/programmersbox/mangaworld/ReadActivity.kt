package com.programmersbox.mangaworld

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
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
import com.programmersbox.mangaworld.utils.batteryAlertPercent
import com.programmersbox.rxutils.invoke
import com.programmersbox.rxutils.toLatestFlowable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Flowables
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.activity_read.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

class ReadActivity : AppCompatActivity() {

    private val disposable = CompositeDisposable()
    private var model: ChapterModel? = null
    private var mangaTitle: String? = null
    private val loader by lazy { Glide.with(this) }
    private val adapter by lazy {
        loader.let {
            PageAdapter(
                fullRequest = it
                    .asDrawable()
                    .skipMemoryCache(true)
                    .centerCrop(),
                thumbRequest = it
                    .asDrawable()
                    .diskCacheStrategy(DiskCacheStrategy.DATA)
                    .transition(withCrossFade()),
                context = this@ReadActivity,
                dataList = mutableListOf()
            ) { image ->
                requestPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE) { p ->
                    if (p.isGranted) saveImage("${mangaTitle}_${model?.name}_${image.toUri().lastPathSegment}", image)
                }
            }
        }
    }

    private var batteryInfo: BroadcastReceiver? = null
    private var timeTicker: BroadcastReceiver? = null

    private val batteryLevelAlert = PublishSubject.create<Float>()
    private val batteryInfoItem = PublishSubject.create<Battery>()

    enum class BatteryViewType(val icon: GoogleMaterial.Icon) {
        CHARGING_FULL(GoogleMaterial.Icon.gmd_battery_charging_full),
        DEFAULT(GoogleMaterial.Icon.gmd_battery_std),
        FULL(GoogleMaterial.Icon.gmd_battery_full),
        ALERT(GoogleMaterial.Icon.gmd_battery_alert),
        UNKNOWN(GoogleMaterial.Icon.gmd_battery_unknown)
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_read)

        hideSystemUI()

        val preloader: RecyclerViewPreloader<String> = RecyclerViewPreloader(loader, adapter, ViewPreloadSizeProvider(), 10)
        readView.addOnScrollListener(preloader)
        readView.setItemViewCacheSize(0)

        batteryInformation.startDrawable = IconicsDrawable(this, GoogleMaterial.Icon.gmd_battery_std).apply {
            colorInt = Color.WHITE
            sizePx = batteryInformation.textSize.roundToInt()
        }

        Flowables.combineLatest(
            batteryLevelAlert
                .map { it <= batteryAlertPercent }
                .map { if (it) Color.RED else Color.WHITE }
                .toLatestFlowable(),
            batteryInfoItem
                .map {
                    when {
                        it.isCharging -> BatteryViewType.CHARGING_FULL
                        it.percent <= batteryAlertPercent -> BatteryViewType.ALERT
                        it.percent >= 95 -> BatteryViewType.FULL
                        it.health == BatteryHealth.UNKNOWN -> BatteryViewType.UNKNOWN
                        else -> BatteryViewType.DEFAULT
                    }
                }
                .distinctUntilChanged { t1, t2 -> t1 != t2 }
                .map { IconicsDrawable(this, it.icon).apply { sizePx = batteryInformation.textSize.roundToInt() } }
                .toLatestFlowable()
        )
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                it.second.colorInt = it.first
                batteryInformation.startDrawable = it.second
                batteryInformation.setTextColor(it.first)
                batteryInformation.startDrawable?.setTint(it.first)
            }
            .addTo(disposable)

        batteryInfo = battery {
            batteryInformation.text = "${it.percent.toInt()}%"
            batteryLevelAlert(it.percent)
            batteryInfoItem(it)
        }
        timeTicker = timeTick { _, _ -> currentTime.text = SimpleDateFormat("HH:mm a", Locale.getDefault()).format(System.currentTimeMillis()) }
        currentTime.text = SimpleDateFormat("HH:mm a", Locale.getDefault()).format(System.currentTimeMillis())

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

        mangaTitle = intent.getStringExtra("mangaTitle")
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

    private fun saveImage(filename: String, downloadUrlOfImage: String) {
        val direct = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!.absolutePath + "/" + "MangaWorld" + "/")

        if (!direct.exists()) {
            direct.mkdir()
        }

        val request = DownloadDslManager(this) {
            downloadUri = Uri.parse(downloadUrlOfImage)
            allowOverRoaming = true
            networkType = DownloadDslManager.NetworkType.WIFI_MOBILE
            title = mangaTitle ?: filename
            mimeType = "image/jpeg"
            visibility = DownloadDslManager.NotificationVisibility.VISIBLE
            destinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, File.separator + "MangaWorld" + File.separator.toString() + filename)
        }

        downloadManager.enqueue(request)
    }

    private fun hideSystemUI() {
        // Set the IMMERSIVE flag.
        // Set the content to appear under the system bars so that the content
        // doesn't resize when the system bars hide and show.
        val decorView = window.decorView
        decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LOW_PROFILE
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                or View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                or View.SYSTEM_UI_FLAG_IMMERSIVE)
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
