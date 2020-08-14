package com.programmersbox.mangaworld

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModel
import androidx.palette.graphics.Palette
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.programmersbox.dragswipe.DragSwipeAdapter
import com.programmersbox.dragswipe.get
import com.programmersbox.flowutils.collectOnUi
import com.programmersbox.flowutils.invoke
import com.programmersbox.gsonutils.getObjectExtra
import com.programmersbox.helpfulutils.*
import com.programmersbox.loggingutils.Loged
import com.programmersbox.manga_db.MangaDatabase
import com.programmersbox.manga_sources.mangasources.ChapterModel
import com.programmersbox.manga_sources.mangasources.MangaInfoModel
import com.programmersbox.manga_sources.mangasources.MangaModel
import com.programmersbox.mangaworld.adapters.ChapterHolder
import com.programmersbox.mangaworld.adapters.ChapterListAdapter
import com.programmersbox.mangaworld.databinding.ActivityMangaBinding
import com.programmersbox.mangaworld.utils.*
import com.programmersbox.thirdpartyutils.ChromeCustomTabTransformationMethod
import com.programmersbox.thirdpartyutils.changeTint
import com.programmersbox.thirdpartyutils.check
import com.programmersbox.thirdpartyutils.getPalette
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_manga.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class MangaActivity : AppCompatActivity() {

    private val dao by lazy { MangaDatabase.getInstance(this).mangaDao() }
    private var range: MutableItemRange<String> = MutableItemRange()
    private val disposable = CompositeDisposable()
    private var mangaModel: MangaModel? = null
    private val listener = FirebaseDb.FirebaseListener()
    private var dialog: AlertDialog? = null

    @Suppress("EXPERIMENTAL_API_USAGE")
    private var isFavorite = MutableStateFlow(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivityMangaBinding = DataBindingUtil.setContentView(this@MangaActivity, R.layout.activity_manga)

        mangaModel = intent.getObjectExtra<MangaModel>("manga", null)

        isFavorite.collectOnUi {
            favoriteManga.check(it)
            favoriteInfo.text = getText(if (it) R.string.removeFromFavorites else R.string.addToFavorites)
        }

        loadMangaInfo(binding, mangaModel)
    }

    private fun loadMangaInfo(binding: ActivityMangaBinding, manga: MangaModel?) {
        GlobalScope.launch {
            try {
                val model = MangaInfoCache.getInfo()?.find { it.mangaUrl == manga?.mangaUrl } ?: manga?.toInfoModel()?.also(MangaInfoCache::newInfo)
                val swatch = if (usePalette)
                    intent.getObjectExtra<Palette.Swatch>("swatch", null) ?: model?.imageUrl?.let(this@MangaActivity::getBitmapFromURL)
                        ?.getPalette()?.vibrantSwatch
                else null
                runOnUiThread {
                    moreInfoSetup(swatch)
                    binding.info = model
                    binding.swatch = swatch?.let { SwatchInfo(it.rgb, it.titleTextColor, it.bodyTextColor) }
                    binding.presenter = this@MangaActivity
                    mangaSetup(model, swatch)
                    dbLoad(manga)
                }
            } catch (e: Exception) {
                FirebaseCrashlytics.getInstance().recordException(e)
                runOnUiThread {
                    dialog = MaterialAlertDialogBuilder(this@MangaActivity)
                        .setTitle(R.string.wentWrong)
                        .setMessage(getString(R.string.wentWrongManga, manga?.title))
                        .setPositiveButton(R.string.ok) { d, _ ->
                            d.dismiss()
                            finish()
                        }
                        .create()
                    dialog?.show()
                }
            }
        }
    }

    private fun getBitmapFromURL(strURL: String?): Bitmap? = try {
        val url = URL(strURL)
        val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
        connection.doInput = true
        connection.connect()
        BitmapFactory.decodeStream(connection.inputStream)
    } catch (e: IOException) {
        e.printStackTrace()
        null
    }

    private fun dbLoad(manga: MangaModel?) {
        favoriteManga.setOnClickListener {

            fun addManga(mangaModel: MangaModel, count: Int) {
                Completable.concatArray(
                    FirebaseDb.addManga2(mangaModel, count),
                    dao.insertManga(mangaModel.toMangaDbModel(count)).subscribeOn(Schedulers.io())
                )
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { isFavorite(true) }
                    .addTo(disposable)
                /*FirebaseDb.addManga2(mangaModel, count)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { isFavorite(true) }
                    .addTo(disposable)
                dao.insertManga(mangaModel.toMangaDbModel(count))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { isFavorite(true) }
                    .addTo(disposable)*/
            }

            fun removeManga(mangaModel: MangaModel) {
                Completable.concatArray(
                    FirebaseDb.removeManga2(mangaModel),
                    dao.deleteManga(mangaModel.toMangaDbModel()).subscribeOn(Schedulers.io())
                )
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { isFavorite(false) }
                    .addTo(disposable)
                /* FirebaseDb.removeManga2(mangaModel)
                     .subscribeOn(Schedulers.io())
                     .observeOn(AndroidSchedulers.mainThread())
                     .subscribe { isFavorite(false) }
                     .addTo(disposable)
                 dao.deleteManga(mangaModel.toMangaDbModel())
                     .subscribeOn(Schedulers.io())
                     .observeOn(AndroidSchedulers.mainThread())
                     .subscribe { isFavorite(false) }
                     .addTo(disposable)*/
            }

            manga?.let {
                if (isFavorite()) removeManga(it) else addManga(it, (mangaInfoChapterList.adapter as? DragSwipeAdapter<*, *>)?.itemCount ?: 0)
            }
        }

        manga?.mangaUrl?.let {
            dao.getMangaById(it)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(onSuccess = { isFavorite(true) }, onError = { isFavorite(false) })
                .addTo(disposable)

            /*FirebaseDb.findMangaByUrlSingle(it)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(onSuccess = isFavorite::invoke, onError = { isFavorite(false) })
                .addTo(disposable)*/

            listener.findMangaByUrlFlowable(it)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(isFavorite::invoke)
                .addTo(disposable)
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun mangaSetup(mangaInfoModel: MangaInfoModel?, swatch: Palette.Swatch?) {
        Loged.r(mangaInfoModel)
        mangaInfoModel?.let { manga ->
            range.itemList.addAll(listOf(manga.title, *manga.alternativeNames.toTypedArray()).filter(String::isNotEmpty))
            swatch?.rgb?.let { mangaInfoLayout.setBackgroundColor(it) }
            val adapter = ChapterListAdapter(
                dataList = manga.chapters.toMutableList(), context = this@MangaActivity, swatch = swatch, rv = mangaInfoChapterList,
                mangaTitle = manga.title, mangaUrl = manga.mangaUrl, dao = dao, isAdult = mangaModel?.source?.isAdult == true
            ) { ChapterHistory(mangaUrl = manga.mangaUrl, imageUrl = manga.imageUrl, title = manga.title, chapterModel = it) }

            mangaInfoChapterList.adapter = adapter
            mangaInfoChapterList.setItemViewCacheSize(20)
            mangaInfoChapterList.setHasFixedSize(true)

            /*GlobalScope.launch {
                val f = FirebaseDb.getChapters(manga)
                Loged.f(f?.joinToString("\n"))
            }*/

            dbAndFireChapter(manga.mangaUrl)
                // dao.getReadChaptersById(manga.mangaUrl)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map { it.filter { m -> m.mangaUrl == manga.mangaUrl } }
                .distinct { it }
                .subscribe { adapter.update(it) { c, m -> c.url == m.url } }//readLoad(it) }
                .addTo(disposable)

            /*DragSwipeUtils.setDragSwipeUp(
                mangaInfoChapterList,
                ReadOrMarkRead(
                    mangaInfoChapterList.adapter as ChapterListAdapter,
                    Direction.NOTHING.value,
                    listOf(Direction.START, Direction.END).let { it.drop(1).fold(it.first().value) { acc, s -> acc + s } },
                    this@MangaActivity,
                    swatch
                ),
                dragSwipeActions = DragSwipeActionBuilder {
                    onSwiped { viewHolder, direction, adapter ->
                        when (direction) {
                            //read
                            Direction.START -> viewHolder.itemView.performClick()
                            //add/remove
                            Direction.END -> (viewHolder as ChapterHolder).readChapter.let { it.isChecked = !it.isChecked }
                            else -> Unit
                        }
                        adapter.notifyItemChanged(viewHolder.adapterPosition)
                    }
                }
            )*/

            FastScrollerBuilder(mangaInfoChapterList)
                .useMd2Style()
                .whatIfNotNull(getDrawable(R.drawable.afs_md2_thumb)) { drawable ->
                    swatch?.bodyTextColor?.let { drawable.changeDrawableColor(it) }
                    setThumbDrawable(drawable)
                }
                .build()

            mangaUrl.transformationMethod = ChromeCustomTabTransformationMethod(this) {
                setStartAnimations(this@MangaActivity, R.anim.slide_in_right, R.anim.slide_out_left)
                swatch?.rgb?.let { setToolbarColor(it) }
                addDefaultShareMenuItem()
            }
            mangaUrl.movementMethod = LinkMovementMethod.getInstance()
        }
    }

    fun titles() = run { mangaInfoTitle.text = range.inc().item }

    fun markRead(model: MangaInfoModel?) {
        GlobalScope.launch {
            val read = model?.mangaUrl?.let { dbAndFireChapterNonFlow(it, dao) }.orEmpty()//let(dao::getReadChaptersByIdNonFlow).orEmpty()
            val adapter = mangaInfoChapterList.adapter as ChapterListAdapter
            runOnUiThread {
                MaterialAlertDialogBuilder(this@MangaActivity)
                    .setTitle("Mark as Read/Unread")
                    .setMultiChoiceItems(
                        adapter.dataList.map(ChapterModel::name).toTypedArray(),
                        BooleanArray(adapter.itemCount) { i -> read.any { it.url == adapter[i].url } }
                    ) { _, index, _ -> (mangaInfoChapterList.findViewHolderForAdapterPosition(index) as? ChapterHolder)?.readChapter?.performClick() }
                    .setPositiveButton("Done") { d, _ -> d.dismiss() }
                    .show()
            }
        }
    }

    fun shareManga(manga: MangaInfoModel?) {
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, manga?.mangaUrl)
            putExtra(Intent.EXTRA_TITLE, manga?.title)
        }, "Share ${manga?.title}"))
    }

    private fun moreInfoSetup(swatch: Palette.Swatch?) {
        var set = ConstraintRangeSet(
            mangaInfoFullLayout,
            ConstraintRanges(
                mangaInfoFullLayout,
                ConstraintSet().apply { clone(mangaInfoFullLayout) },
                ConstraintSet().apply { clone(this@MangaActivity, R.layout.activity_manga_alt) }
            ),
            ConstraintRanges(
                mangaInfoLayout,
                ConstraintSet().apply { clone(mangaInfoLayout) },
                ConstraintSet().apply { clone(this@MangaActivity, R.layout.manga_info_layout_alt) }
            )
        )
        moreInfo.setOnClickListener { set++ }
        colorSetup(swatch)
    }

    private fun colorSetup(swatch: Palette.Swatch?) {
        swatch?.rgb?.let { moreInfo.setBackgroundColor(it) }
        swatch?.titleTextColor?.let { moreInfo.setTextColor(it) }
        swatch?.rgb?.let {
            markChapters.strokeColor = ColorStateList.valueOf(it)
            markChapters.setTextColor(it)
        }
        swatch?.rgb?.let {
            shareButton.strokeColor = ColorStateList.valueOf(it)
            shareButton.iconTint = ColorStateList.valueOf(it)
        }
        favoriteManga.changeTint(swatch?.rgb ?: Color.WHITE)

        swatch?.rgb?.let { window.statusBarColor = it }
    }

    private class ConstraintRangeSet(private val rootLayout: ConstraintLayout, vararg items: ConstraintRanges) : Range<ConstraintRanges>() {

        override val itemList: List<ConstraintRanges> = items.toList()

        override operator fun inc(): ConstraintRangeSet {
            super.inc()
            rootLayout.animateChildren {
                itemList.forEach {
                    it.inc()
                    it.item.applyTo(it.layout)
                }
            }
            return this
        }

        override operator fun dec(): ConstraintRangeSet {
            super.dec()
            rootLayout.animateChildren {
                itemList.forEach {
                    it.dec()
                    it.item.applyTo(it.layout)
                }
            }
            return this
        }

        override fun onChange(current: Int, item: ConstraintRanges) = Unit
    }

    private class ConstraintRanges(val layout: ConstraintLayout, vararg items: ConstraintSet) : Range<ConstraintSet>() {
        override val itemList: List<ConstraintSet> = items.toList()
        override fun onChange(current: Int, item: ConstraintSet) = Unit
    }

    override fun onDestroy() {
        dialog?.dismiss()
        listener.listener?.remove()
        FirebaseDb.detachChapterListener()
        disposable.dispose()
        super.onDestroy()
    }
}

@BindingAdapter("coverImage")
fun loadImage(view: ImageView, imageUrl: String?) {
    Glide.with(view)
        .load(imageUrl)
        .override(360, 480)
        .placeholder(R.drawable.manga_world_round_logo)
        .error(R.drawable.manga_world_round_logo)
        .fallback(R.drawable.manga_world_round_logo)
        .transform(RoundedCorners(15))
        .into(view)
}

@BindingAdapter("otherNames")
fun otherNames(view: TextView, names: List<String>?) {
    view.text = names?.joinToString("\n\n")
}

@BindingAdapter("genreList", "swatch")
fun loadGenres(view: ChipGroup, genres: List<String>?, swatchInfo: SwatchInfo?) {
    genres?.forEach {
        view.addView(Chip(view.context).apply {
            text = it
            isCheckable = false
            isClickable = false
            swatchInfo?.rgb?.let { setTextColor(it) }
            swatchInfo?.bodyColor?.let { chipBackgroundColor = ColorStateList.valueOf(it) }
        })
    }
}

@BindingAdapter("titleColor")
fun titleColor(view: TextView, swatchInfo: SwatchInfo?) {
    swatchInfo?.titleColor?.let { view.setTextColor(it) }
}

@BindingAdapter("bodyColor")
fun bodyColor(view: TextView, swatchInfo: SwatchInfo?) {
    swatchInfo?.bodyColor?.let { view.setTextColor(it) }
}

@BindingAdapter("linkColor")
fun linkColor(view: TextView, swatchInfo: SwatchInfo?) {
    swatchInfo?.bodyColor?.let { view.setLinkTextColor(it) }
}

data class SwatchInfo(val rgb: Int?, val titleColor: Int?, val bodyColor: Int?) : ViewModel()

class OverScrollBehavior(context: Context, attributeSet: AttributeSet) : CoordinatorLayout.Behavior<View>() {

    companion object {
        private const val OVER_SCROLL_AREA = 4
    }

    private var overScrollY = 0

    override fun onStartNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: View,
        directTargetChild: View,
        target: View,
        axes: Int,
        type: Int
    ): Boolean {
        overScrollY = 0
        return true
    }

    override fun onNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: View,
        target: View,
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        type: Int,
        consumed: IntArray
    ) {
        if (dyUnconsumed == 0) {
            return
        }

        overScrollY -= (dyUnconsumed / OVER_SCROLL_AREA)
        val group = target as ViewGroup
        val count = group.childCount
        for (i in 0 until count) {
            val view = group.getChildAt(i)
            view.translationY = overScrollY.toFloat()
        }
    }

    override fun onStopNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: View,
        target: View,
        type: Int
    ) {
        // Smooth animate to 0 when the user stops scrolling
        moveToDefPosition(target)
    }

    override fun onNestedPreFling(
        coordinatorLayout: CoordinatorLayout,
        child: View,
        target: View,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        // Scroll view by inertia when current position equals to 0
        if (overScrollY == 0) {
            return false
        }
        // Smooth animate to 0 when user fling view
        moveToDefPosition(target)
        return true
    }

    private fun moveToDefPosition(target: View) {
        val group = target as ViewGroup
        val count = group.childCount
        for (i in 0 until count) {
            val view = group.getChildAt(i)
            ViewCompat.animate(view)
                .translationY(0f)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
        }
    }
}