package com.programmersbox.mangaworld

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
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
                runOnUiThread {
                    val swatch = if (usePalette) intent.getObjectExtra<Palette.Swatch>("swatch", null) else null
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
