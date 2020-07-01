package com.programmersbox.mangaworld.adapters

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestBuilder
import com.github.piasy.biv.indicator.progresspie.ProgressPieIndicator
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.programmersbox.helpfulutils.layoutInflater
import com.programmersbox.mangaworld.R
import com.programmersbox.thirdpartyutils.DragSwipeGlideAdapter
import kotlinx.android.synthetic.main.page_item.view.*
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
                    .setTitle("Download page?")
                    .setPositiveButton("Yes") { d, _ -> canDownload(item!!);d.dismiss() }
                    .setNegativeButton("No") { d, _ -> d.dismiss() }
                    .show()
            } catch (e: Exception) {
            }
            true
        }
    }
}
