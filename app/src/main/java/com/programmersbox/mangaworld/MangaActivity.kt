package com.programmersbox.mangaworld

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
import coil.api.load
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.programmersbox.flowutils.collectOnUi
import com.programmersbox.flowutils.invoke
import com.programmersbox.gsonutils.getObjectExtra
import com.programmersbox.helpfulutils.*
import com.programmersbox.loggingutils.Loged
import com.programmersbox.manga_db.MangaDatabase
import com.programmersbox.manga_sources.mangasources.MangaInfoModel
import com.programmersbox.manga_sources.mangasources.MangaModel
import com.programmersbox.mangaworld.adapters.ChapterListAdapter
import com.programmersbox.mangaworld.databinding.ActivityMangaBinding
import com.programmersbox.mangaworld.utils.toMangaDbModel
import com.programmersbox.mangaworld.utils.usePalette
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
    private var range: ItemRange<String> = ItemRange()
    private val disposable = CompositeDisposable()

    @Suppress("EXPERIMENTAL_API_USAGE")
    private var isFavorite = MutableStateFlow(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivityMangaBinding = DataBindingUtil.setContentView(this@MangaActivity, R.layout.activity_manga)

        val manga = intent.getObjectExtra<MangaModel>("manga", null)

        isFavorite.collectOnUi { favoriteManga.check(it) }
        isFavorite.collectOnUi { favoriteInfo.text = if (it) "Remove from Favorites" else "Add to Favorites" }

        favoriteInfo.setOnClickListener { favoriteManga.performClick() }

        loadMangaInfo(binding, manga)
    }

    private fun loadMangaInfo(binding: ActivityMangaBinding, manga: MangaModel?) {
        GlobalScope.launch {
            val model = manga?.toInfoModel()
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
            manga?.toMangaDbModel()
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
        GlobalScope.launch {
            val read = mangaInfoModel?.mangaUrl?.let { dao.getReadChaptersByIdNonFlow(it) }
            runOnUiThread {
                Loged.r(mangaInfoModel)
                mangaInfoModel?.let { manga ->
                    range.itemList.addAll(manga.title, *manga.alternativeNames.toTypedArray())
                    swatch?.rgb?.let { mangaInfoLayout.setBackgroundColor(it) }
                    mangaInfoChapterList.adapter = ChapterListAdapter(
                        dataList = manga.chapters.toMutableList(), context = this@MangaActivity, swatch = swatch,
                        mangaUrl = mangaInfoModel.mangaUrl, dao = dao, chapters = read
                    )
                    FastScrollerBuilder(mangaInfoChapterList)
                        .useMd2Style()
                        .whatIfNotNull(getDrawable(R.drawable.afs_md2_thumb)) { drawable ->
                            swatch?.rgb?.let { drawable.changeDrawableColor(it) }
                            setThumbDrawable(drawable)
                        }
                        .build()
                }
            }
        }

    }

    fun titles() {
        range++
        mangaInfoTitle.text = range.item
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
    }

    private class ConstraintRangeSet(private val rootLayout: ConstraintLayout, vararg items: ConstraintRanges, loop: Boolean = true) :
        ItemRange<ConstraintRanges>(*items, loop = loop) {
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
    }

    private class ConstraintRanges(val layout: ConstraintLayout, vararg items: ConstraintSet, loop: Boolean = true) :
        ItemRange<ConstraintSet>(*items, loop = loop)

    override fun onDestroy() {
        disposable.dispose()
        super.onDestroy()
    }
}

@BindingAdapter("coverImage")
fun loadImage(view: ImageView, imageUrl: String?) {
    view.load(imageUrl) {
        size(360, 480)
        placeholder(R.mipmap.ic_launcher)
        error(R.mipmap.ic_launcher)
        crossfade(true)
    }
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
