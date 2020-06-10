package com.programmersbox.mangaworld.adapters

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.ListPreloader
import com.bumptech.glide.RequestBuilder
import com.programmersbox.helpfulutils.layoutInflater
import com.programmersbox.mangaworld.R
import com.programmersbox.thirdpartyutils.DragSwipeGlideAdapter
import kotlinx.android.synthetic.main.page_item.view.*

class PageAdapter(
    override val fullRequest: RequestBuilder<Drawable>,
    override val thumbRequest: RequestBuilder<Drawable>,
    private val context: Context,
    dataList: MutableList<String>
) : DragSwipeGlideAdapter<String, PageHolder, String>(dataList), ListPreloader.PreloadModelProvider<String> {

    override val itemToModel: (String) -> String = { it }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageHolder =
        PageHolder(context.layoutInflater.inflate(R.layout.page_item, parent, false))

    override fun PageHolder.onBind(item: String, position: Int) = render(item)
}

class PageHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val image = itemView.chapterPage!!

    fun render(item: String?) {
        image.showImage(Uri.parse(item))
    }
}
