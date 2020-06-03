package com.programmersbox.mangaworld

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.jakewharton.rxbinding2.widget.textChanges
import com.programmersbox.dragswipe.*
import com.programmersbox.manga_db.MangaDatabase
import com.programmersbox.manga_db.MangaDbModel
import com.programmersbox.manga_sources.mangasources.MangaModel
import com.programmersbox.manga_sources.mangasources.Sources
import com.programmersbox.mangaworld.adapters.GalleryListAdapter
import com.programmersbox.mangaworld.utils.toMangaDbModel
import com.programmersbox.mangaworld.utils.toMangaModel
import com.programmersbox.mangaworld.views.AutoFitGridLayoutManager
import com.programmersbox.rxutils.behaviorDelegate
import com.programmersbox.rxutils.listMap
import com.programmersbox.rxutils.toLatestFlowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Flowables
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import kotlinx.android.synthetic.main.activity_favorite.*
import java.util.concurrent.TimeUnit

class FavoriteActivity : AppCompatActivity() {
    private val disposable = CompositeDisposable()
    private val adapter = GalleryListAdapter(this, disposable, false)
    private val sourcePublisher = BehaviorSubject.createDefault(mutableListOf(*Sources.values()))
    private var sourcesList by behaviorDelegate(sourcePublisher)
    private val dao by lazy { MangaDatabase.getInstance(this).mangaDao() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorite)

        uiSetup()

        Flowables.combineLatest(
            source1 = dao.getAllManga()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .listMap(MangaDbModel::toMangaModel),
            source2 = sourcePublisher.toLatestFlowable(),
            source3 = favorite_search_info
                .textChanges()
                .debounce(500, TimeUnit.MILLISECONDS)
                .toLatestFlowable()
        )
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .map(mapManga)
            .subscribe(adapter::setData)
            .addTo(disposable)
    }

    private fun uiSetup() {
        favoriteMangaRV.layoutManager = AutoFitGridLayoutManager(this, 360).apply { orientation = GridLayoutManager.VERTICAL }
        favoriteMangaRV.adapter = adapter

        DragSwipeUtils.setDragSwipeUp(
            adapter, favoriteMangaRV,
            swipeDirs = listOf(Direction.START, Direction.END),
            dragSwipeActions = DragSwipeActionBuilder {
                onSwiped { viewHolder, _, dragSwipeAdapter ->
                    val item = dragSwipeAdapter[viewHolder.adapterPosition].toMangaDbModel()
                    addOrRemoveManga(item)
                }
            }
        )

        Sources.values().forEach {
            sourceFilter.addView(Chip(this).apply {
                text = it.name
                isCheckable = true
                isClickable = true
                isChecked = true
                setOnCheckedChangeListener { _, isChecked -> addOrRemoveSource(isChecked, it) }
            })
        }

    }

    private val mapManga: (Triple<List<MangaModel>, List<Sources>, CharSequence>) -> List<MangaModel> = { pair ->
        pair.first.sortedBy(MangaModel::title).filter { it.source in pair.second && it.title.contains(pair.third, true) }
    }

    private fun addOrRemoveSource(isChecked: Boolean, sources: Sources) {
        sourcesList = sourcesList?.apply { if (isChecked) add(sources) else remove(sources) }
    }

    private fun addOrRemoveManga(item: MangaDbModel) = dao
        .deleteManga(item)
        .subscribeOn(Schedulers.io())
        .observeOn(Schedulers.io())
        .subscribe {
            Snackbar.make(favoriteMangaRV, getString(R.string.removed, item.title), Snackbar.LENGTH_LONG)
                .setAction(getText(R.string.undo)) {
                    dao
                        .insertManga(item)
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.io())
                        .subscribe()
                }
                .show()
        }

    override fun onDestroy() {
        disposable.dispose()
        super.onDestroy()
    }
}

fun DragSwipeAdapter<MangaModel, *>.setData(newList: List<MangaModel>) {
    val diffCallback = object : DragSwipeDiffUtil<MangaModel>(dataList, newList) {
        override fun areContentsTheSame(oldItem: MangaModel, newItem: MangaModel): Boolean = oldItem.mangaUrl == newItem.mangaUrl
        override fun areItemsTheSame(oldItem: MangaModel, newItem: MangaModel): Boolean = oldItem.mangaUrl === newItem.mangaUrl
    }
    val diffResult = DiffUtil.calculateDiff(diffCallback)
    dataList.clear()
    dataList.addAll(newList)
    diffResult.dispatchUpdatesTo(this)
}
