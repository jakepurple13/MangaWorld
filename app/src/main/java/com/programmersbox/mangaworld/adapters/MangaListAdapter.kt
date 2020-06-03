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
import com.programmersbox.manga_db.MangaDatabase
import com.programmersbox.manga_sources.mangasources.MangaModel
import com.programmersbox.mangaworld.MangaActivity
import com.programmersbox.mangaworld.R
import com.programmersbox.mangaworld.databinding.MangaListItemBinding
import com.programmersbox.mangaworld.utils.toMangaDbModel
import com.programmersbox.mangaworld.utils.usePalette
import com.programmersbox.thirdpartyutils.changeTint
import com.programmersbox.thirdpartyutils.check
import com.programmersbox.thirdpartyutils.checked
import com.programmersbox.thirdpartyutils.into
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.manga_list_item.view.*

class MangaListAdapter(private val context: Context, private val disposable: CompositeDisposable = CompositeDisposable()) :
    DragSwipeAdapter<MangaModel, MangaHolder>() {

    private val dao by lazy { MangaDatabase.getInstance(context).mangaDao() }

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

        dao.getAllManga()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .map { it.any { model -> model.mangaUrl == item.mangaUrl } }
            .subscribe { favorite.check(it) }
            .addTo(disposable)

        favorite.setOnClickListener {
            item.toMangaDbModel()
                .let { it1 -> if (favorite.progress > 0.9f) dao.deleteManga(it1) else if (!favorite.checked) dao.insertManga(it1) else null }
                ?.subscribeOn(Schedulers.io())
                ?.observeOn(AndroidSchedulers.mainThread())
                ?.subscribe()
                ?.addTo(disposable)
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
                        dom?.titleTextColor?.let { favorite.changeTint(it) }

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
    val favorite = itemView.isFavoriteManga!!

    fun bind(item: MangaModel) {
        binding.model = item
        binding.executePendingBindings()
    }
}
