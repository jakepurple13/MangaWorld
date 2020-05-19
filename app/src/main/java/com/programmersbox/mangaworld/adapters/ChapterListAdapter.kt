package com.programmersbox.mangaworld.adapters

import android.content.Context
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.RecyclerView
import com.programmersbox.dragswipe.DragSwipeAdapter
import com.programmersbox.gsonutils.putExtra
import com.programmersbox.helpfulutils.layoutInflater
import com.programmersbox.manga_sources.mangasources.ChapterModel
import com.programmersbox.mangaworld.R
import com.programmersbox.mangaworld.ReadActivity
import kotlinx.android.synthetic.main.chapter_list_item.view.*

class ChapterListAdapter(private val context: Context, dataList: MutableList<ChapterModel>, private val swatch: Palette.Swatch?) :
    DragSwipeAdapter<ChapterModel, ChapterHolder>(dataList) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChapterHolder =
        ChapterHolder(context.layoutInflater.inflate(R.layout.chapter_list_item, parent, false))

    override fun ChapterHolder.onBind(item: ChapterModel, position: Int) {
        name.text = item.name
        uploaded.text = item.uploaded
        swatch?.rgb?.let { card.setCardBackgroundColor(it) }
        swatch?.titleTextColor?.let { name.setTextColor(it) }
        swatch?.bodyTextColor?.let { uploaded.setTextColor(it) }
        itemView.setOnClickListener {
            context.startActivity(Intent(context, ReadActivity::class.java).apply { putExtra("chapter", item) })
        }
    }
}

class ChapterHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val name = itemView.chapterName!!
    val uploaded = itemView.uploadedInfo!!
    val card = itemView.chapterListCard!!
}