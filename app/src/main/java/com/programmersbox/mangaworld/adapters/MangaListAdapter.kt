package com.programmersbox.mangaworld.adapters

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintSet
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.programmersbox.dragswipe.DragSwipeAdapter
import com.programmersbox.gsonutils.putExtra
import com.programmersbox.helpfulutils.ConstraintRange
import com.programmersbox.helpfulutils.layoutInflater
import com.programmersbox.manga_sources.mangasources.MangaModel
import com.programmersbox.mangaworld.MangaActivity
import com.programmersbox.mangaworld.R
import com.programmersbox.mangaworld.databinding.MangaListItemBinding
import com.programmersbox.mangaworld.utils.usePalette
import com.programmersbox.thirdpartyutils.into
import kotlinx.android.synthetic.main.manga_list_item.view.*

class MangaListAdapter(private val context: Context) : DragSwipeAdapter<MangaModel, MangaHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MangaHolder =
        MangaHolder(MangaListItemBinding.inflate(context.layoutInflater, parent, false))

    override fun MangaHolder.onBind(item: MangaModel, position: Int) {

        var range = ConstraintRange(
            constraintLayout,
            ConstraintSet().apply { clone(constraintLayout) },
            ConstraintSet().apply { clone(context, R.layout.manga_list_item_alt) }
        )

        var swatch: Palette.Swatch? = null

        itemView.setOnClickListener {
            context.startActivity(Intent(context, MangaActivity::class.java).apply {
                putExtra("manga", item)
                putExtra("swatch", swatch)
            })
        }
        itemView.setOnLongClickListener {
            range++
            true
        }
        bind(item)
        Glide.with(cover)
            .asBitmap()
            .load(item.imageUrl)
            .override(360, 480)
            .fallback(R.mipmap.ic_launcher)
            .placeholder(R.mipmap.ic_launcher)
            .error(R.mipmap.ic_launcher)
            .into<Bitmap> {
                resourceReady { image, _ ->
                    cover.setImageBitmap(image)
                    if (context.usePalette) {
                        val p = Palette.from(image).generate()

                        val dom = p.vibrantSwatch
                        dom?.rgb?.let { layout.setCardBackgroundColor(it) }
                        dom?.titleTextColor?.let { title.setTextColor(it) }
                        dom?.bodyTextColor?.let { description.setTextColor(it) }

                        swatch = dom
                    }
                }
            }
    }
}

class MangaHolder(private val binding: MangaListItemBinding) : RecyclerView.ViewHolder(binding.root) {
    val cover = itemView.mangaListCover!!
    val title = itemView.mangaListTitle!!
    val description = itemView.mangaListDescription!!
    val layout = itemView.mangaListLayout!!
    val constraintLayout = itemView.mangaListConstraintLayout!!

    fun bind(item: MangaModel) {
        binding.model = item
        binding.executePendingBindings()
    }
}