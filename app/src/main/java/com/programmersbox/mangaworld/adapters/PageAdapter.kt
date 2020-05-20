package com.programmersbox.mangaworld.adapters

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.api.load
import com.programmersbox.dragswipe.DragSwipeAdapter
import com.programmersbox.helpfulutils.layoutInflater
import com.programmersbox.mangaworld.R
import kotlinx.android.synthetic.main.page_item.view.*

class PageAdapter(private val context: Context, dataList: MutableList<String>) : DragSwipeAdapter<String, PageViewHolder>(dataList) {

    override fun getItemCount(): Int = super.getItemCount() + 1

    override fun getItemViewType(position: Int): Int = when (position) {
        dataList.size + 1 -> 0
        else -> 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder = when (viewType) {
        0 -> PageLoadingHolder(context.layoutInflater.inflate(R.layout.page_item, parent, false))
        else -> PageHolder(context.layoutInflater.inflate(R.layout.page_item, parent, false))
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) = holder.render(dataList.getOrNull(position))
    override fun PageViewHolder.onBind(item: String, position: Int) = Unit//render(item)

}

abstract class PageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    abstract fun render(item: String?)
}

class PageHolder(itemView: View) : PageViewHolder(itemView) {
    private val image = itemView.chapterPage!!

    override fun render(item: String?) {
        image.load(item) {
            error(android.R.drawable.ic_delete)
        }
    }
}

class PageLoadingHolder(itemView: View) : PageViewHolder(itemView) {
    //private val loading = itemView.pageLoading!!

    override fun render(item: String?) {

    }
}
