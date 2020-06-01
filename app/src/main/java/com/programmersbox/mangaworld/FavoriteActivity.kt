package com.programmersbox.mangaworld

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DiffUtil
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.jakewharton.rxbinding2.widget.textChanges
import com.programmersbox.dragswipe.*
import com.programmersbox.manga_db.MangaDatabase
import com.programmersbox.manga_db.MangaDbModel
import com.programmersbox.manga_sources.mangasources.MangaModel
import com.programmersbox.manga_sources.mangasources.Sources
import com.programmersbox.mangaworld.adapters.MangaListAdapter
import com.programmersbox.mangaworld.utils.toMangaDbModel
import com.programmersbox.mangaworld.utils.toMangaModel
import io.reactivex.BackpressureStrategy
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
    private val adapter = MangaListAdapter(this)
    private val sourcePublisher = BehaviorSubject.createDefault(listOf(*Sources.values()))
    private val sourceFilters = mutableListOf(*Sources.values())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorite)

        uiSetup()

        Flowables.combineLatest(
            source1 = MangaDatabase.getInstance(this).mangaDao().getAllManga()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map { it.map(MangaDbModel::toMangaModel) },
            source2 = sourcePublisher.toFlowable(BackpressureStrategy.LATEST),
            source3 = favorite_search_info
                .textChanges()
                .debounce(500, TimeUnit.MILLISECONDS)
                .toFlowable(BackpressureStrategy.LATEST)
        )
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .map(mapManga)
            .subscribe(adapter::setData)
            .addTo(disposable)

    }

    private fun uiSetup() {
        favoriteMangaRV.adapter = adapter

        DragSwipeUtils.setDragSwipeUp(
            adapter,
            favoriteMangaRV,
            swipeDirs = listOf(Direction.START, Direction.END),
            dragSwipeActions = DragSwipeActionBuilder {
                onSwiped { viewHolder, _, dragSwipeAdapter ->
                    val item = dragSwipeAdapter[viewHolder.adapterPosition].toMangaDbModel(0)
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
        pair.first.sortedBy(MangaModel::title).filter {
            (if (pair.second.isEmpty()) true else it.source in pair.second) &&
                    if (pair.third.isBlank()) true else it.title.contains(pair.third, true)
        }
    }

    private fun addOrRemoveSource(isChecked: Boolean, sources: Sources) {
        if (isChecked) sourceFilters.add(sources) else sourceFilters.remove(sources)
        sourcePublisher.onNext(sourceFilters)
    }

    private fun addOrRemoveManga(item: MangaDbModel) =
        MangaDatabase.getInstance(this@FavoriteActivity).mangaDao()
            .deleteManga(item)
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .subscribe {
                Snackbar.make(favoriteMangaRV, "Removed ${item.title}", Snackbar.LENGTH_LONG)
                    .setAction("Undo") {
                        MangaDatabase.getInstance(this@FavoriteActivity).mangaDao()
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
