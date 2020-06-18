package com.programmersbox.mangaworld.adapters

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.core.graphics.ColorUtils
import androidx.databinding.BindingAdapter
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.programmersbox.dragswipe.DragSwipeAdapter
import com.programmersbox.gsonutils.putExtra
import com.programmersbox.helpfulutils.layoutInflater
import com.programmersbox.helpfulutils.whatIfNotNull
import com.programmersbox.manga_db.MangaDao
import com.programmersbox.manga_db.MangaReadChapter
import com.programmersbox.manga_sources.mangasources.ChapterModel
import com.programmersbox.mangaworld.R
import com.programmersbox.mangaworld.ReadActivity
import com.programmersbox.mangaworld.SwatchInfo
import com.programmersbox.mangaworld.databinding.ChapterListItemBinding
import com.programmersbox.mangaworld.utils.ChapterHistory
import com.programmersbox.mangaworld.utils.FirebaseDb
import com.programmersbox.mangaworld.utils.addToHistory
import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.chapter_list_item.view.*

class ChapterListAdapter(
    private val context: Context,
    dataList: MutableList<ChapterModel>,
    swatch: Palette.Swatch?,
    private val mangaTitle: String,
    private val mangaUrl: String,
    private val dao: MangaDao,
    private val toChapterHistory: (ChapterModel) -> ChapterHistory
) : DragSwipeAdapter<ChapterModel, ChapterHolder>(dataList) {

    var chapters: List<MangaReadChapter>? = null

    private val info = swatch?.let { SwatchInfo(it.rgb, it.titleTextColor, it.bodyTextColor) }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChapterHolder =
        ChapterHolder(ChapterListItemBinding.inflate(context.layoutInflater, parent, false))

    override fun ChapterHolder.onBind(item: ChapterModel, position: Int) {
        bind(item, info)
        itemView.setOnClickListener {
            readChapter.isChecked = true
            context.addToHistory(toChapterHistory(item))
            context.startActivity(
                Intent(context, ReadActivity::class.java).apply {
                    //putExtra("chapter", position)
                    //putExtra("nextChapter", dataList)
                    putExtra("mangaTitle", mangaTitle)
                    putExtra("currentChapter", item)
                }
            )
        }

        startReading.setOnClickListener { itemView.performClick() }

        readChapter.setOnCheckedChangeListener(null)
        readChapter.isChecked = false

        readChapter.isChecked = chapters?.any { it.url == item.url } ?: false
        readChapter.setOnCheckedChangeListener { _, isChecked ->
            /*MangaReadChapter(item.url, item.name, mangaUrl)
                .also { if (isChecked) FirebaseDb.addChapter(it) else FirebaseDb.removeChapter(it) }
                .let { if (isChecked) dao.insertChapter(it) else dao.deleteChapter(it) }*/
            MangaReadChapter(item.url, item.name, mangaUrl)
                .let {
                    Completable.mergeArray(
                        if (isChecked) FirebaseDb.addChapter(it) else FirebaseDb.removeChapter(it),
                        if (isChecked) dao.insertChapter(it) else dao.deleteChapter(it)
                    )
                }
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe {
                    Snackbar.make(
                        itemView,
                        context.getString(if (isChecked) R.string.addChapter else R.string.removeChapter, item.name),
                        Snackbar.LENGTH_SHORT
                    )
                        .whatIfNotNull(info?.rgb) { setTextColor(it) }
                        .whatIfNotNull(info?.rgb) { setActionTextColor(it) }
                        .whatIfNotNull(info?.bodyColor) { setBackgroundTint(ColorUtils.setAlphaComponent(it, 0xff)) }
                        .setAction(context.getText(R.string.undo)) { readChapter.isChecked = !isChecked }
                        .show()
                }
        }
    }
}

class ChapterHolder(private val binding: ChapterListItemBinding) : RecyclerView.ViewHolder(binding.root) {
    val readChapter = itemView.readChapter!!
    val startReading = itemView.startReading!!

    //val chapterName = itemView.chapterName!!
    fun bind(item: ChapterModel, swatchInfo: SwatchInfo?) {
        binding.chapter = item
        binding.swatch = swatchInfo
        binding.executePendingBindings()
    }
}

@BindingAdapter("checkedButtonTint")
fun buttonTint(view: CheckBox, swatchInfo: SwatchInfo?) {
    swatchInfo?.bodyColor?.let { view.buttonTintList = ColorStateList.valueOf(it) }
}