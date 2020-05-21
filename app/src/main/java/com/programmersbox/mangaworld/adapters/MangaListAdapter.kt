package com.programmersbox.mangaworld.adapters

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintSet
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.RecyclerView
import coil.api.load
import coil.bitmappool.BitmapPool
import coil.size.Size
import coil.transform.Transformation
import com.programmersbox.dragswipe.DragSwipeAdapter
import com.programmersbox.gsonutils.putExtra
import com.programmersbox.helpfulutils.ConstraintRange
import com.programmersbox.helpfulutils.layoutInflater
import com.programmersbox.manga_sources.mangasources.MangaModel
import com.programmersbox.mangaworld.MangaActivity
import com.programmersbox.mangaworld.R
import com.programmersbox.mangaworld.databinding.MangaListItemBinding
import com.programmersbox.mangaworld.utils.usePalette
import kotlinx.android.synthetic.main.manga_list_item.view.*

class MangaListAdapter(private val context: Context) : DragSwipeAdapter<MangaModel, MangaHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MangaHolder {
        return MangaHolder(MangaListItemBinding.inflate(context.layoutInflater, parent, false))
    }

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
        cover.load(item.imageUrl) {
            size(360, 480)
            placeholder(R.mipmap.ic_launcher)
            error(R.mipmap.ic_launcher)
            crossfade(true)
            if (context.usePalette) {
                transformations(object : Transformation {
                    override fun key() = "paletteTransformer"
                    override suspend fun transform(pool: BitmapPool, input: Bitmap, size: Size): Bitmap {
                        val p = Palette.from(input).generate()

                        val dom = p.vibrantSwatch
                        dom?.rgb?.let { layout.setCardBackgroundColor(it) }
                        dom?.titleTextColor?.let { title.setTextColor(it) }
                        dom?.bodyTextColor?.let { description.setTextColor(it) }

                        swatch = dom

                        return input
                    }
                })
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