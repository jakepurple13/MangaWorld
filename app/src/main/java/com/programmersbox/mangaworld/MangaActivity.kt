package com.programmersbox.mangaworld

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
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
import com.programmersbox.dragswipe.*
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
import com.programmersbox.mangaworld.utils.ChapterHistory
import com.programmersbox.mangaworld.utils.MangaInfoCache
import com.programmersbox.mangaworld.utils.toMangaDbModel
import com.programmersbox.mangaworld.utils.usePalette
import com.programmersbox.mangaworld.views.ReadOrMarkRead
import com.programmersbox.thirdpartyutils.changeTint
import com.programmersbox.thirdpartyutils.check
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
    private var adapter: ChapterListAdapter? = null

    @Suppress("EXPERIMENTAL_API_USAGE")
    private var isFavorite = MutableStateFlow(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivityMangaBinding = DataBindingUtil.setContentView(this@MangaActivity, R.layout.activity_manga)

        val manga = intent.getObjectExtra<MangaModel>("manga", null)

        isFavorite.collectOnUi { favoriteManga.check(it) }
        isFavorite.collectOnUi { favoriteInfo.text = getText(if (it) R.string.removeFromFavorites else R.string.addToFavorites) }

        loadMangaInfo(binding, manga)
    }

    private fun loadMangaInfo(binding: ActivityMangaBinding, manga: MangaModel?) {
        GlobalScope.launch {
            val model = MangaInfoCache.getInfo()?.find { it.mangaUrl == manga?.mangaUrl } ?: manga?.toInfoModel()?.also(MangaInfoCache::newInfo)
            runOnUiThread {
                val swatch = if (usePalette) intent.getObjectExtra<Palette.Swatch>("swatch", null) else null
                moreInfoSetup(swatch)
                binding.info = model
                binding.swatch = swatch?.let { SwatchInfo(it.rgb, it.titleTextColor, it.bodyTextColor) }
                binding.presenter = this@MangaActivity
                mangaSetup(model, swatch)
                favoriteManga.changeTint(swatch?.rgb ?: Color.WHITE)
                dbLoad(manga)
            }
        }
    }

    private fun dbLoad(manga: MangaModel?) {
        favoriteManga.setOnClickListener {
            manga?.toMangaDbModel((mangaInfoChapterList.adapter as? DragSwipeAdapter<*, *>)?.itemCount ?: 0)
                ?.let { it1 -> if (isFavorite()) dao.deleteManga(it1) else if (!isFavorite()) dao.insertManga(it1) else null }
                ?.subscribeOn(Schedulers.io())
                ?.observeOn(AndroidSchedulers.mainThread())
                ?.subscribe { isFavorite(!isFavorite()) }
        }

        manga?.mangaUrl?.let {
            dao.getMangaById(it)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(onSuccess = { isFavorite(true) }, onError = { isFavorite(false) })
                .addTo(disposable)
        }
    }

    private fun mangaSetup(mangaInfoModel: MangaInfoModel?, swatch: Palette.Swatch?) {
        Loged.r(mangaInfoModel)
        mangaInfoModel?.let { manga ->
            range.itemList.addAll(listOf(manga.title, *manga.alternativeNames.toTypedArray()).filter(String::isNotEmpty))
            swatch?.rgb?.let { mangaInfoLayout.setBackgroundColor(it) }
            adapter = ChapterListAdapter(
                dataList = manga.chapters.toMutableList(), context = this@MangaActivity, swatch = swatch,
                mangaUrl = manga.mangaUrl, dao = dao
            ) { ChapterHistory(mangaUrl = manga.mangaUrl, imageUrl = manga.imageUrl, title = manga.title, chapterModel = it) }

            mangaInfoChapterList.adapter = adapter

            dao.getReadChaptersById(manga.mangaUrl)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    adapter?.chapters = it
                    adapter?.notifyDataSetChanged()
                }
                .addTo(disposable)

            DragSwipeUtils.setDragSwipeUp(
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
            )

            FastScrollerBuilder(mangaInfoChapterList)
                .useMd2Style()
                .whatIfNotNull(getDrawable(R.drawable.afs_md2_thumb)) { drawable ->
                    swatch?.bodyTextColor?.let { drawable.changeDrawableColor(it) }
                    setThumbDrawable(drawable)
                }
                .build()
        }
    }

    fun titles() = run { mangaInfoTitle.text = range.inc().item }

    fun markRead(model: MangaInfoModel?) {
        GlobalScope.launch {
            val read = model?.mangaUrl?.let(dao::getReadChaptersByIdNonFlow).orEmpty()
            val adapter = mangaInfoChapterList.adapter as ChapterListAdapter
            runOnUiThread {
                MaterialAlertDialogBuilder(this@MangaActivity)
                    .setTitle("Mark as Read/Unread")
                    .setMultiChoiceItems(
                        adapter.dataList.map(ChapterModel::name).toTypedArray(),
                        BooleanArray(adapter.itemCount) { i -> read.any { it.url == adapter[i].url } }) { _, index, _ ->
                        (mangaInfoChapterList.findViewHolderForAdapterPosition(index) as? ChapterHolder)?.readChapter?.performClick()
                    }
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

    private class ConstraintRanges(val layout: ConstraintLayout, vararg items: ConstraintSet, loop: Boolean = true) : Range<ConstraintSet>() {
        override val itemList: List<ConstraintSet> = items.toList()
        override fun onChange(current: Int, item: ConstraintSet) = Unit
    }

    override fun onDestroy() {
        disposable.dispose()
        super.onDestroy()
    }
}

@BindingAdapter("coverImage")
fun loadImage(view: ImageView, imageUrl: String?) {
    Glide.with(view)
        .load(imageUrl)
        .override(360, 480)
        .placeholder(R.mipmap.ic_launcher)
        .error(R.mipmap.ic_launcher)
        .fallback(R.mipmap.ic_launcher)
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
