package com.programmersbox.mangaworld

import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModel
import androidx.palette.graphics.Palette
import coil.api.load
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.programmersbox.flowutils.collectOnUi
import com.programmersbox.flowutils.invoke
import com.programmersbox.gsonutils.getObjectExtra
import com.programmersbox.helpfulutils.ItemRange
import com.programmersbox.helpfulutils.addAll
import com.programmersbox.helpfulutils.animateChildren
import com.programmersbox.loggingutils.Loged
import com.programmersbox.manga_sources.mangasources.MangaInfoModel
import com.programmersbox.manga_sources.mangasources.MangaModel
import com.programmersbox.mangaworld.adapters.ChapterListAdapter
import com.programmersbox.mangaworld.databinding.ActivityMangaBinding
import kotlinx.android.synthetic.main.activity_manga.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MangaActivity : AppCompatActivity() {

    private var range: ItemRange<String> = ItemRange()

    @Suppress("EXPERIMENTAL_API_USAGE")
    private var isFavorite = MutableStateFlow(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GlobalScope.launch {
            val model = intent.getObjectExtra<MangaModel>("manga", null)?.toInfoModel()
            runOnUiThread {
                val binding: ActivityMangaBinding = DataBindingUtil.setContentView(this@MangaActivity, R.layout.activity_manga)
                val swatch = intent.getObjectExtra<Palette.Swatch>("swatch", null)
                moreInfoSetup(swatch)
                binding.info = model
                binding.swatch = SwatchInfo(swatch?.rgb, swatch?.titleTextColor, swatch?.bodyTextColor)
                binding.presenter = this@MangaActivity
                mangaSetup(model, swatch)
                favoriteManga.changeTint(swatch?.rgb ?: Color.WHITE)
                isFavorite
                    .map { if (it) 1f else 0f }
                    .map { ValueAnimator.ofFloat(favoriteManga.progress, it) }
                    .collectOnUi {
                        it.addUpdateListener { animation: ValueAnimator -> favoriteManga.progress = animation.animatedValue as Float }
                        it.start()
                    }
                isFavorite.collectOnUi { favoriteInfo.text = if (it) "Remove from Favorites" else "Add to Favorites" }
                isFavorite.collectOnUi { Toast.makeText(this@MangaActivity, "Work in Progress...", Toast.LENGTH_SHORT).show() }
                //on this click you will send the request to favorite or unfavorite
                //on reply, update isFavorite
                //OR
                //change isFavorite here and when this activity is opened, get the response from wherever to check if this is in favorite
                favoriteManga.setOnClickListener { isFavorite(!isFavorite()) }
                favoriteInfo.setOnClickListener { favoriteManga.performClick() }
            }
        }
    }

    private fun LottieAnimationView.changeTint(newColor: Int) =
        addValueCallback(KeyPath("**"), LottieProperty.COLOR_FILTER) { PorterDuffColorFilter(newColor, PorterDuff.Mode.SRC_ATOP) }

    private fun mangaSetup(mangaInfoModel: MangaInfoModel?, swatch: Palette.Swatch?) {
        Loged.r(mangaInfoModel)
        mangaInfoModel?.let { manga ->
            range.itemList.addAll(manga.title, *manga.alternativeNames.toTypedArray())
            swatch?.rgb?.let { mangaInfoLayout.setBackgroundColor(it) }
            mangaInfoChapterList.adapter = ChapterListAdapter(dataList = manga.chapters.toMutableList(), context = this, swatch = swatch)
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
}

@BindingAdapter("coverImage")
fun loadImage(view: ImageView, imageUrl: String) {
    view.load(imageUrl) {
        size(360, 480)
        placeholder(R.mipmap.ic_launcher)
        error(R.mipmap.ic_launcher)
        crossfade(true)
    }
}

@BindingAdapter("otherNames")
fun otherNames(view: TextView, names: List<String>) {
    view.text = names.joinToString("\n\n")
}

@BindingAdapter("genreList", "swatch")
fun loadGenres(view: ChipGroup, genres: List<String>, swatchInfo: SwatchInfo) {
    genres.forEach {
        view.addView(Chip(view.context).apply {
            text = it
            isCheckable = false
            isClickable = false
            swatchInfo.rgb?.let { setTextColor(it) }
            swatchInfo.bodyColor?.let { chipBackgroundColor = ColorStateList.valueOf(it) }
        })
    }
}

@BindingAdapter("titleColor")
fun titleColor(view: TextView, swatchInfo: SwatchInfo) {
    swatchInfo.titleColor?.let { view.setTextColor(it) }
}

@BindingAdapter("bodyColor")
fun bodyColor(view: TextView, swatchInfo: SwatchInfo) {
    swatchInfo.bodyColor?.let { view.setTextColor(it) }
}

@BindingAdapter("linkColor")
fun linkColor(view: TextView, swatchInfo: SwatchInfo) {
    swatchInfo.bodyColor?.let { view.setLinkTextColor(it) }
}

data class SwatchInfo(val rgb: Int?, val titleColor: Int?, val bodyColor: Int?) : ViewModel()