package com.programmersbox.mangaworld.adapters

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestBuilder
import com.github.piasy.biv.indicator.progresspie.ProgressPieIndicator
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.programmersbox.helpfulutils.layoutInflater
import com.programmersbox.helpfulutils.runOnUIThread
import com.programmersbox.manga_db.MangaDatabase
import com.programmersbox.manga_db.MangaReadChapter
import com.programmersbox.manga_sources.mangasources.ChapterModel
import com.programmersbox.mangaworld.R
import com.programmersbox.mangaworld.utils.FirebaseDb
import com.programmersbox.thirdpartyutils.DragSwipeGlideAdapter
import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.page_end_chapter_item.view.*
import kotlinx.android.synthetic.main.page_item.view.*
import kotlinx.android.synthetic.main.page_next_chapter_item.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

class PageAdapter(
    override val fullRequest: RequestBuilder<Drawable>,
    override val thumbRequest: RequestBuilder<Drawable>,
    private val context: Context,
    dataList: MutableList<String>,
    private val canDownload: (String) -> Unit = { }
) : DragSwipeGlideAdapter<String, PageHolder, String>(dataList) {

    override val itemToModel: (String) -> String = { it }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageHolder =
        PageHolder(context.layoutInflater.inflate(R.layout.page_item, parent, false))

    override fun PageHolder.onBind(item: String, position: Int) = render(item, canDownload)

    override fun getPreloadItems(position: Int): List<String> = Collections.singletonList(dataList[position].let(itemToModel))
}

class PageHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val image = itemView.chapterPage!!

    fun render(item: String?, canDownload: (String) -> Unit) {
        image.setProgressIndicator(ProgressPieIndicator())
        image.showImage(Uri.parse(item), Uri.parse(item))
        image.setOnLongClickListener {
            try {
                MaterialAlertDialogBuilder(itemView.context)
                    .setTitle(itemView.context.getText(R.string.downloadPage))
                    .setPositiveButton(itemView.context.getText(android.R.string.ok)) { d, _ -> canDownload(item!!);d.dismiss() }
                    .setNegativeButton(itemView.context.getText(R.string.fui_cancel)) { d, _ -> d.dismiss() }
                    .show()
            } catch (e: Exception) {
            }
            true
        }
    }
}

class PageAdapter2(
    override val fullRequest: RequestBuilder<Drawable>,
    override val thumbRequest: RequestBuilder<Drawable>,
    private val activity: AppCompatActivity,
    dataList: MutableList<String>,
    private val chapterModels: List<ChapterModel>,
    var currentChapter: Int,
    private val mangaUrl: String,
    private val loadNewPages: (ChapterModel) -> Unit = {},
    private val canDownload: (String) -> Unit = { }
) : DragSwipeGlideAdapter<String, Page2Holder, String>(dataList) {

    private val context: Context = activity

    private val dao by lazy { MangaDatabase.getInstance(context).mangaDao() }

    override val itemToModel: (String) -> String = { it }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Page2Holder = context.layoutInflater.inflate(viewType, parent, false).let {
        when (viewType) {
            R.layout.page_end_chapter_item -> Page2Holder.LastChapterHolder(it)
            R.layout.page_next_chapter_item -> Page2Holder.LoadNextChapterHolder(it)
            R.layout.page_item -> Page2Holder.ReadingHolder(it)
            else -> Page2Holder.ReadingHolder(it)
        }
    }

    override fun getItemCount(): Int = super.getItemCount() + 1

    override fun getItemViewType(position: Int): Int = when {
        position == dataList.size && currentChapter <= 0 -> R.layout.page_end_chapter_item
        position == dataList.size -> R.layout.page_next_chapter_item
        else -> R.layout.page_item
    }

    override fun onBindViewHolder(holder: Page2Holder, position: Int) {
        when (holder) {
            is Page2Holder.ReadingHolder -> holder.render(dataList[position], canDownload)
            is Page2Holder.LoadNextChapterHolder -> {
                holder.render {
                    runOnUIThread {
                        chapterModels.getOrNull(--currentChapter)?.let(loadNewPages)
                        chapterModels.getOrNull(currentChapter)?.let { item ->
                            MangaReadChapter(item.url, item.name, mangaUrl)
                                .let {
                                    Completable.mergeArray(
                                        FirebaseDb.addChapter(it),
                                        dao.insertChapter(it)
                                    )
                                }
                                .subscribeOn(Schedulers.io())
                                .observeOn(Schedulers.io())
                                .subscribe()
                        }
                    }
                }
            }
            is Page2Holder.LastChapterHolder -> holder.render(activity)
        }
    }

    override fun Page2Holder.onBind(item: String, position: Int) = Unit

    fun reloadChapter() {
        chapterModels.getOrNull(currentChapter)?.let(loadNewPages)
    }

    //override fun getPreloadItems(position: Int): List<String> = Collections.singletonList(dataList[position].let(itemToModel))

    override fun getPreloadItems(position: Int): List<String> = Collections.singletonList(dataList.getOrNull(position)?.let(itemToModel).orEmpty())

}

sealed class Page2Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    class LastChapterHolder(itemView: View) : Page2Holder(itemView) {
        private val returnButton = itemView.goBackFromReading!!
        fun render(activity: AppCompatActivity) {
            returnButton.setOnClickListener { activity.finish() }
        }
    }

    class ReadingHolder(itemView: View) : Page2Holder(itemView) {
        private val image = itemView.chapterPage!!

        fun render(item: String?, canDownload: (String) -> Unit) {
            image.setProgressIndicator(ProgressPieIndicator())
            image.showImage(Uri.parse(item), Uri.parse(item))
            image.setOnLongClickListener {
                try {
                    MaterialAlertDialogBuilder(itemView.context)
                        .setTitle(itemView.context.getText(R.string.downloadPage))
                        .setPositiveButton(itemView.context.getText(android.R.string.ok)) { d, _ -> canDownload(item!!);d.dismiss() }
                        .setNegativeButton(itemView.context.getText(R.string.fui_cancel)) { d, _ -> d.dismiss() }
                        .show()
                } catch (e: Exception) {
                }
                true
            }
        }
    }

    class LoadNextChapterHolder(itemView: View) : Page2Holder(itemView) {
        private val loadButton = itemView.loadNextChapter!!

        fun render(load: suspend () -> Unit) {
            loadButton.setOnClickListener { GlobalScope.launch { load() } }
        }
    }

}

