package com.programmersbox.mangaworld.adapters

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Environment
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.databinding.BindingAdapter
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.perfomer.blitz.setTimeAgo
import com.programmersbox.dragswipe.CheckAdapter
import com.programmersbox.dragswipe.CheckAdapterInterface
import com.programmersbox.dragswipe.DragSwipeAdapter
import com.programmersbox.gsonutils.putExtra
import com.programmersbox.helpfulutils.days
import com.programmersbox.helpfulutils.isDateBetween
import com.programmersbox.helpfulutils.layoutInflater
import com.programmersbox.helpfulutils.whatIfNotNull
import com.programmersbox.loggingutils.Loged
import com.programmersbox.loggingutils.f
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
import com.programmersbox.mangaworld.utils.useAgo
import com.tonyodev.fetch2.Fetch
import com.tonyodev.fetch2.NetworkType
import com.tonyodev.fetch2.Priority
import com.tonyodev.fetch2.Request
import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.chapter_list_item.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File

class ChapterListAdapter(
    private val context: Context,
    dataList: MutableList<ChapterModel>,
    swatch: Palette.Swatch?,
    private val mangaTitle: String,
    private val mangaUrl: String,
    private val dao: MangaDao,
    private val isAdult: Boolean,
    check: CheckAdapter<ChapterModel, MangaReadChapter> = CheckAdapter(),
    private val toChapterHistory: (ChapterModel) -> ChapterHistory
) : DragSwipeAdapter<ChapterModel, ChapterHolder>(dataList), CheckAdapterInterface<ChapterModel, MangaReadChapter> by check {

    init {
        check.adapter = this
    }

    /*private val chapters: MutableList<MangaReadChapter> = mutableListOf()

    private val previousList = mutableListOf<MangaReadChapter>()
    fun readLoad(list: List<MangaReadChapter>) {
        val mapNotNull: (Int) -> Int? = { if (it == -1) null else it }
        previousList.clear()
        previousList.addAll(chapters)
        list.map(previousList::indexOf).mapNotNull(mapNotNull).forEach(this::notifyItemChanged)
        chapters.clear()
        chapters.addAll(list)
        list.map { l -> dataList.indexOfFirst { it.url == l.url } }.mapNotNull(mapNotNull).forEach(this::notifyItemChanged)
    }*/

    private val info = swatch?.let { SwatchInfo(it.rgb, it.titleTextColor, it.bodyTextColor) }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChapterHolder =
        ChapterHolder(ChapterListItemBinding.inflate(context.layoutInflater, parent, false))

    override fun ChapterHolder.onBind(item: ChapterModel, position: Int) {
        bind(item, info)
        itemView.setOnClickListener {
            readChapter.isChecked = true
            if (!isAdult) context.addToHistory(toChapterHistory(item))
            context.startActivity(
                Intent(context, ReadActivity::class.java).apply {
                    //putExtra("chapter", position)
                    //putExtra("nextChapter", dataList)
                    putExtra("mangaTitle", mangaTitle)
                    putExtra("currentChapter", item)
                    putExtra("mangaUrl", item.url)
                    putExtra("allChapters", dataList)
                    putExtra("mangaInfoUrl", mangaUrl)
                }
            )
            menu.resetStatus()
        }

        startReading.setOnClickListener { itemView.performClick() }
        layout.setOnClickListener { itemView.performClick() }

        readChapterButton.setOnClickListener { itemView.performClick() }

        downloadChapterButton.setOnClickListener {
            MaterialAlertDialogBuilder(context)
                .setTitle("Download Chapter?")
                .setPositiveButton("Yes") { d, _ ->
                    menu.resetStatus()
                    readChapter.isChecked = true
                    GlobalScope.launch { downloadChapter(item, mangaTitle) }
                    d.dismiss()
                }
                .setNegativeButton("No") { d, _ -> d.dismiss() }
                .show()
        }

        markButton.setOnClickListener { readChapter.isChecked = !readChapter.isChecked }

        readChapter.setOnCheckedChangeListener(null)
        readChapter.isChecked = false

        readChapter.isChecked = currentList.any { it.url == item.url }// ?: false
        readChapter.setOnCheckedChangeListener { v, isChecked ->
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
                        v,
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

    private suspend fun downloadChapter(model: ChapterModel, title: String) {
        val fileLocation = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + "/MangaWorld/"
        val direct = File("$fileLocation$title/${model.name}/")

        if (!direct.exists()) {
            direct.mkdir()
        }

        val pages = model.getPageInfo().pages.mapIndexed { index, s ->
            Request(s, direct.absolutePath + "/$index.png").apply {
                priority = Priority.HIGH
                networkType = NetworkType.ALL
                model.sources.headers.forEach { addHeader(it.first, it.second) }
            }
        }

        Loged.f(pages.map { it.file })

        Fetch.getDefaultInstance().enqueue(pages)
    }
}

class ChapterHolder(private val binding: ChapterListItemBinding) : RecyclerView.ViewHolder(binding.root) {
    val readChapter = itemView.readChapter!!
    val startReading = itemView.startReading!!
    val readChapterButton = itemView.readChapterButton!!
    val downloadChapterButton = itemView.downloadChapterButton!!
    val markButton = itemView.markedReadButton!!
    val menu = itemView.swipeMenu!!
    val layout = itemView.chapterListCard!!

    //val chapterName = itemView.chapterName!!
    fun bind(item: ChapterModel, swatchInfo: SwatchInfo?) {
        binding.chapter = item
        binding.swatch = swatchInfo
        binding.executePendingBindings()
    }
}

@BindingAdapter("optionTint")
fun optionTint(view: MaterialButton, swatchInfo: SwatchInfo?) {
    swatchInfo?.rgb?.let { view.strokeColor = ColorStateList.valueOf(it) }
}

@BindingAdapter("checkedButtonTint")
fun buttonTint(view: CheckBox, swatchInfo: SwatchInfo?) {
    swatchInfo?.bodyColor?.let { view.buttonTintList = ColorStateList.valueOf(it) }
}

@BindingAdapter("uploadedText")
fun uploadedText(view: TextView, chapterModel: ChapterModel) {
    if (
        view.context.useAgo &&
        chapterModel.uploadedTime != null &&
        chapterModel.uploadedTime?.isDateBetween(System.currentTimeMillis() - 8.days.inMilliseconds.toLong(), System.currentTimeMillis()) == true
    ) {
        view.setTimeAgo(chapterModel.uploadedTime!!, showSeconds = true, autoUpdate = false)
    } else {
        view.text = chapterModel.uploaded
    }
}
