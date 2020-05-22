package com.programmersbox.mangaworld.adapters

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.api.load
import com.programmersbox.dragswipe.DragSwipeAdapter
import com.programmersbox.helpfulutils.gone
import com.programmersbox.helpfulutils.layoutInflater
import com.programmersbox.helpfulutils.visible
import com.programmersbox.mangaworld.R
import kotlinx.android.synthetic.main.page_item.view.*

class PageAdapter(private val context: Context, dataList: MutableList<String>) : DragSwipeAdapter<String, PageHolder>(dataList) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageHolder =
        PageHolder(context.layoutInflater.inflate(R.layout.page_item, parent, false))

    override fun PageHolder.onBind(item: String, position: Int) = render(item)
}

class PageHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val image = itemView.chapterPage!!
    private val loading = itemView.loading!!

    fun render(item: String?) {
        image.load(item) {
            error(android.R.drawable.ic_delete)
            target(
                onSuccess = {
                    image.setImageDrawable(it)
                    loading.gone()
                },
                onError = {
                    image.setImageDrawable(it)
                    loading.gone()
                },
                onStart = {
                    image.setImageDrawable(it)
                    loading.visible()
                }
            )
        }
    }
}
