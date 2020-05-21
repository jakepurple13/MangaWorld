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
import com.programmersbox.mangaworld.SwatchInfo
import com.programmersbox.mangaworld.databinding.ChapterListItemBinding

class ChapterListAdapter(private val context: Context, dataList: MutableList<ChapterModel>, private val swatch: Palette.Swatch?) :
    DragSwipeAdapter<ChapterModel, ChapterHolder>(dataList) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChapterHolder =
        ChapterHolder(ChapterListItemBinding.inflate(context.layoutInflater, parent, false))

    override fun ChapterHolder.onBind(item: ChapterModel, position: Int) {
        bind(item, SwatchInfo(swatch?.rgb, swatch?.titleTextColor, swatch?.bodyTextColor))
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
    fun bind(item: ChapterModel, swatchInfo: SwatchInfo) {
        binding.chapter = item
        binding.swatch = swatchInfo
        binding.executePendingBindings()
    }
}