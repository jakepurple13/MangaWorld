package com.programmersbox.mangaworld.adapters

import android.content.Context
import android.content.Intent
import android.view.ViewGroup
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.RecyclerView
import com.programmersbox.dragswipe.DragSwipeAdapter
import com.programmersbox.gsonutils.putExtra
import com.programmersbox.helpfulutils.layoutInflater
import com.programmersbox.manga_sources.mangasources.ChapterModel
import com.programmersbox.mangaworld.ReadActivity
import com.programmersbox.mangaworld.databinding.ChapterListItemBinding
import kotlinx.android.synthetic.main.chapter_list_item.view.*

class ChapterListAdapter(private val context: Context, dataList: MutableList<ChapterModel>, private val swatch: Palette.Swatch?) :
    DragSwipeAdapter<ChapterModel, ChapterHolder>(dataList) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChapterHolder =
        ChapterHolder(ChapterListItemBinding.inflate(context.layoutInflater, parent, false))

    override fun ChapterHolder.onBind(item: ChapterModel, position: Int) {
        bind(item)
        swatch?.rgb?.let { card.setCardBackgroundColor(it) }
        swatch?.titleTextColor?.let { name.setTextColor(it) }
        swatch?.bodyTextColor?.let { uploaded.setTextColor(it) }
        itemView.setOnClickListener {
            context.startActivity(
                Intent(context, ReadActivity::class.java).apply {
                    //putExtra("chapter", position)
                    //putExtra("nextChapter", dataList)
                    putExtra("currentChapter", item)
                }
            )
        }
    }
}

class ChapterHolder(private val binding: ChapterListItemBinding) : RecyclerView.ViewHolder(binding.root) {
    val name = itemView.chapterName!!
    val uploaded = itemView.uploadedInfo!!
    val card = itemView.chapterListCard!!

    fun bind(item: ChapterModel) {
        binding.chapter = item
        binding.executePendingBindings()
    }
}