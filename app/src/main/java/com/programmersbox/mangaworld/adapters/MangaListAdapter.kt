package com.programmersbox.mangaworld.adapters

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.constraintlayout.widget.ConstraintSet
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.programmersbox.dragswipe.DragSwipeAdapter
import com.programmersbox.gsonutils.putExtra
import com.programmersbox.helpfulutils.ConstraintRange
import com.programmersbox.helpfulutils.Range
import com.programmersbox.helpfulutils.gone
import com.programmersbox.helpfulutils.layoutInflater
import com.programmersbox.manga_db.MangaDatabase
import com.programmersbox.manga_sources.mangasources.MangaModel
import com.programmersbox.mangaworld.MangaActivity
import com.programmersbox.mangaworld.R
import com.programmersbox.mangaworld.databinding.MangaListItemBinding
import com.programmersbox.mangaworld.databinding.MangaListItemGalleryViewBinding
import com.programmersbox.mangaworld.utils.FirebaseDb
import com.programmersbox.mangaworld.utils.dbAndFireManga
import com.programmersbox.mangaworld.utils.toMangaDbModel
import com.programmersbox.mangaworld.utils.usePalette
import com.programmersbox.thirdpartyutils.changeTint
import com.programmersbox.thirdpartyutils.check
import com.programmersbox.thirdpartyutils.checked
import com.programmersbox.thirdpartyutils.into
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.manga_list_item.view.*
import kotlinx.android.synthetic.main.manga_list_item_gallery_view.view.*

abstract class MangaViewAdapter<VH : RecyclerView.ViewHolder>(
    protected val context: Context,
    protected val disposable: CompositeDisposable = CompositeDisposable()
) : DragSwipeAdapter<MangaModel, VH>()

class MangaListAdapter(context: Context, disposable: CompositeDisposable = CompositeDisposable()) :
    MangaViewAdapter<MangaHolder>(context, disposable) {

    private val dao by lazy { MangaDatabase.getInstance(context).mangaDao() }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MangaHolder =
        MangaHolder(MangaListItemBinding.inflate(context.layoutInflater, parent, false))

    override fun MangaHolder.onBind(item: MangaModel, position: Int) {

        var range: Range<ConstraintSet> = ConstraintRange(
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

        context.dbAndFireManga(dao)
            /*dao.getAllManga()*/
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .map { it.any { model -> model.mangaUrl == item.mangaUrl } }
            .subscribe { favorite.check(it) }
            .addTo(disposable)

        favorite.setOnClickListener {
            Completable.mergeArray(
                if (favorite.progress > 0.9f) FirebaseDb.removeManga(item) else if (!favorite.checked) FirebaseDb.addManga(item) else null,
                item.toMangaDbModel()
                    .let { it1 -> if (favorite.progress > 0.9f) dao.deleteManga(it1) else if (!favorite.checked) dao.insertManga(it1) else null }
            )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe()
                .addTo(disposable)
        }

        bind(item)
        Glide.with(cover)
            .asBitmap()
            .load(item.imageUrl)
            .override(360, 480)
            .transform(RoundedCorners(30))
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

class BubbleListAdapter(context: Context, disposable: CompositeDisposable = CompositeDisposable()) :
    MangaViewAdapter<MangaHolder>(context, disposable) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MangaHolder =
        MangaHolder(MangaListItemBinding.inflate(context.layoutInflater, parent, false))

    override fun MangaHolder.onBind(item: MangaModel, position: Int) {

        var range: Range<ConstraintSet> = ConstraintRange(
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
            favorite.gone()
            true
        }

        favorite.gone()

        bind(item)
        Glide.with(cover)
            .asBitmap()
            .load(item.imageUrl)
            .override(360, 480)
            .transform(RoundedCorners(30))
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

class GalleryListAdapter(context: Context, disposable: CompositeDisposable = CompositeDisposable(), private val showMenu: Boolean = true) :
    MangaViewAdapter<GalleryHolder>(context, disposable) {

    private val dao by lazy { MangaDatabase.getInstance(context).mangaDao() }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GalleryHolder =
        GalleryHolder(MangaListItemGalleryViewBinding.inflate(context.layoutInflater, parent, false))

    override fun GalleryHolder.onBind(item: MangaModel, position: Int) {

        var swatch: Palette.Swatch? = null

        itemView.setOnClickListener {
            context.startActivity(Intent(context, MangaActivity::class.java).apply {
                putExtra("manga", item)
                putExtra("swatch", swatch)
            })
        }

        if (showMenu) {
            val menu = PopupMenu(context, itemView)
                .apply {
                    setOnMenuItemClickListener {
                        Completable.mergeArray(
                            when (it.itemId) {
                                1 -> FirebaseDb.addManga(item)
                                2 -> FirebaseDb.removeManga(item)
                                else -> null
                            },
                            when (it.itemId) {
                                1 -> dao.insertManga(item.toMangaDbModel())
                                2 -> dao.deleteManga(item.toMangaDbModel())
                                else -> null
                            }
                        )
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe()
                            .addTo(disposable)
                        true
                    }
                }

            itemView.setOnLongClickListener {
                menu.show()
                true
            }

            dao.getAllManga()
                //context.dbAndFireManga(dao)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map { it.any { model -> model.mangaUrl == item.mangaUrl } }
                .subscribe {
                    menu.menu.clear()
                    if (it) menu.menu.removeItem(1) else menu.menu.removeItem(2)
                    menu.menu.add(
                        1,
                        if (it) 2 else 1,
                        1,
                        if (it) context.getText(R.string.removeFromFavorites) else context.getText(R.string.addToFavorites)
                    )
                }
                .addTo(disposable)
        }

        bind(item)
        Glide.with(context)
            .asBitmap()
            .load(item.imageUrl)
            //.override(360, 480)
            .fitCenter()
            .transform(RoundedCorners(15))
            .fallback(R.mipmap.ic_launcher)
            .placeholder(R.mipmap.ic_launcher)
            .error(R.mipmap.ic_launcher)
            .into<Bitmap> {
                resourceReady { image, _ ->
                    cover.setImageBitmap(image)
                    if (context.usePalette) {
                        swatch = Palette.from(image).generate().vibrantSwatch
                    }
                }
            }
    }
}

class GalleryHolder(private val binding: MangaListItemGalleryViewBinding) : RecyclerView.ViewHolder(binding.root) {
    val cover = itemView.galleryListCover!!
    val title = itemView.galleryListTitle!!
    val layout = itemView.galleryListLayout!!

    fun bind(item: MangaModel) {
        binding.model = item
        binding.executePendingBindings()
    }
}

class GalleryListFavoriteAdapter(private val context: Context) : DragSwipeAdapter<Pair<String, List<MangaModel>>, GalleryHolder>() {

    private val dao by lazy { MangaDatabase.getInstance(context).mangaDao() }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GalleryHolder =
        GalleryHolder(MangaListItemGalleryViewBinding.inflate(context.layoutInflater, parent, false))

    private val listToArray: (List<MangaModel>) -> Array<out String> = { list -> list.map { "${it.source.name} - ${it.title}" }.toTypedArray() }

    override fun GalleryHolder.onBind(item: Pair<String, List<MangaModel>>, position: Int) {

        val manga = item.second.random()

        var swatch: Palette.Swatch? = null

        itemView.setOnClickListener {

            fun startActivity(mangaModel: MangaModel) {
                context.startActivity(Intent(context, MangaActivity::class.java).apply {
                    putExtra("manga", mangaModel)
                    putExtra("swatch", swatch)
                })
            }

            if (item.second.size > 1) {
                MaterialAlertDialogBuilder(context)
                    .setTitle("Select ${item.first} Source")
                    .setItems(listToArray(item.second)) { d, index ->
                        startActivity(item.second[index])
                        d.dismiss()
                    }
                    .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
                    .show()
            } else {
                startActivity(item.second.first())
            }
        }

        itemView.setOnLongClickListener {
            if (item.second.size > 1) {
                MaterialAlertDialogBuilder(context)
                    .setTitle("Remove Source from Favorites")
                    .setItems(listToArray(item.second)) { d, index ->
                        addOrRemoveManga(itemView, item.second[index])
                        d.dismiss()
                    }
                    .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
                    .show()
            } else {
                PopupMenu(context, itemView)
                    .apply {
                        menu.add(1, 1, 1, context.getText(R.string.removeFromFavorites))
                        setOnMenuItemClickListener {
                            if (it.itemId == 1) addOrRemoveManga(itemView, item.second.first())
                            true
                        }
                    }.show()
            }
            true
        }

        bind(manga)
        Glide.with(context)
            .asBitmap()
            .load(manga.imageUrl)
            //.override(360, 480)
            .fitCenter()
            .transform(RoundedCorners(15))
            .fallback(R.mipmap.ic_launcher)
            .placeholder(R.mipmap.ic_launcher)
            .error(R.mipmap.ic_launcher)
            .into<Bitmap> {
                resourceReady { image, _ ->
                    cover.setImageBitmap(image)
                    if (context.usePalette) {
                        swatch = Palette.from(image).generate().vibrantSwatch
                    }
                }
            }
    }

    private fun addOrRemoveManga(view: View, item: MangaModel) = Completable.mergeArray(
        FirebaseDb.removeManga(item),
        dao.deleteManga(item.toMangaDbModel())
    )
        .subscribeOn(Schedulers.io())
        .observeOn(Schedulers.io())
        .subscribe {
            Snackbar.make(view, context.getString(R.string.removed, item.title), Snackbar.LENGTH_LONG)
                .setAction(context.getText(R.string.undo)) {
                    Completable.mergeArray(
                        FirebaseDb.addManga(item),
                        dao.insertManga(item.toMangaDbModel())
                    )
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.io())
                        .subscribe()
                }
                .show()
        }
}
