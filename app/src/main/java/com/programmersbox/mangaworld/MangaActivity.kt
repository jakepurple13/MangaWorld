package com.programmersbox.mangaworld

import android.content.res.ColorStateList
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintSet
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import androidx.palette.graphics.Palette
import coil.api.load
import com.google.android.material.chip.Chip
import com.programmersbox.gsonutils.getObjectExtra
import com.programmersbox.helpfulutils.ConstraintRange
import com.programmersbox.helpfulutils.ItemRange
import com.programmersbox.loggingutils.Loged
import com.programmersbox.manga_sources.mangasources.MangaInfoModel
import com.programmersbox.manga_sources.mangasources.MangaModel
import com.programmersbox.mangaworld.adapters.ChapterListAdapter
import com.programmersbox.mangaworld.databinding.ActivityMangaBinding
import kotlinx.android.synthetic.main.activity_manga.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MangaActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GlobalScope.launch {
            val model = intent.getObjectExtra<MangaModel>("manga", null)?.toInfoModel()
            runOnUiThread {
                val binding: ActivityMangaBinding = DataBindingUtil.setContentView(this@MangaActivity, R.layout.activity_manga)
                val swatch = intent.getObjectExtra<Palette.Swatch>("swatch", null)
                moreInfoSetup(swatch)
                binding.info = model
                mangaSetup(model, swatch)
            }
        }
    }

    private fun mangaSetup(mangaInfoModel: MangaInfoModel?, swatch: Palette.Swatch?) {
        Loged.r(mangaInfoModel)
        mangaInfoModel?.let { manga ->
            swatch?.rgb?.let { mangaInfoLayout.setBackgroundColor(it) }
            swatch?.titleTextColor?.let { mangaInfoTitle.setTextColor(it) }
            swatch?.bodyTextColor?.let { mangaInfoDescription.setTextColor(it) }
            manga.genres.forEach {
                genreList.addView(Chip(this@MangaActivity).apply {
                    text = it
                    isCheckable = false
                    isClickable = false
                    swatch?.rgb?.let { setTextColor(it) }
                    swatch?.bodyTextColor?.let { chipBackgroundColor = ColorStateList.valueOf(it) }
                })
            }
            var range = ItemRange(manga.title, *manga.alternativeNames.toTypedArray())
            mangaInfoTitle.setOnClickListener {
                range++
                mangaInfoTitle.text = range.item
            }
            mangaInfoChapterList.adapter = ChapterListAdapter(dataList = manga.chapters.toMutableList(), context = this, swatch = swatch)
        }
    }

    private fun moreInfoSetup(swatch: Palette.Swatch?) {
        var range = ConstraintRange(
            mangaInfoLayout,
            ConstraintSet().apply { clone(mangaInfoLayout) },
            ConstraintSet().apply { clone(this@MangaActivity, R.layout.manga_info_layout_alt) }
        )
        moreInfo.setOnClickListener { range++ }
        swatch?.rgb?.let { moreInfo.setBackgroundColor(it) }
        swatch?.titleTextColor?.let { moreInfo.setTextColor(it) }
    }
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