package com.programmersbox.mangaworld

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.chip.Chip
import com.jakewharton.rxbinding2.widget.textChanges
import com.programmersbox.dragswipe.DragSwipeAdapter
import com.programmersbox.dragswipe.DragSwipeDiffUtil
import com.programmersbox.helpfulutils.groupByCondition
import com.programmersbox.helpfulutils.similarity
import com.programmersbox.manga_db.MangaDatabase
import com.programmersbox.manga_db.MangaDbModel
import com.programmersbox.manga_sources.mangasources.MangaModel
import com.programmersbox.manga_sources.mangasources.Sources
import com.programmersbox.mangaworld.adapters.GalleryFavoriteAdapter
import com.programmersbox.mangaworld.utils.FirebaseDb
import com.programmersbox.mangaworld.utils.groupManga
import com.programmersbox.mangaworld.utils.toMangaModel
import com.programmersbox.mangaworld.views.AutoFitGridLayoutManager
import com.programmersbox.rxutils.behaviorDelegate
import com.programmersbox.rxutils.toLatestFlowable
import io.reactivex.Flowable
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

    private val sourcePublisher = BehaviorSubject.createDefault(mutableListOf(*Sources.values()))
    private var sourcesList by behaviorDelegate(sourcePublisher)
    private val dao by lazy { MangaDatabase.getInstance(this).mangaDao() }

    private val fireListener = FirebaseDb.FirebaseListener()

    private val adapterType by lazy {
        if (groupManga) GalleryFavoriteAdapter.GalleryGroupAdapter(this, dao)
        else GalleryFavoriteAdapter.GalleryListingAdapter(this, disposable, false, dao)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorite)

        uiSetup()

        val fired = fireListener.getAllMangaFlowable()

        val dbFire = Flowables.combineLatest(
            fired,
            dao.getAllManga()
        ) { db, fire -> (db + fire).groupBy(MangaDbModel::mangaUrl).map { it.value.maxBy(MangaDbModel::numChapters)!! }.map { it.toMangaModel() } }

        Flowables.combineLatest(
            source1 = dbFire
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()),
            source2 = sourcePublisher.toLatestFlowable(),
            source3 = favorite_search_info
                .textChanges()
                .debounce(500, TimeUnit.MILLISECONDS)
                .toLatestFlowable()
        )
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .let { if (groupManga) it.toGroup() else it.toListing() }
            .addTo(disposable)

        /*Flowables.combineLatest(
            source1 = dbAndFireManga2(dao)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()),
            source2 = sourcePublisher.toLatestFlowable(),
            source3 = favorite_search_info
                .textChanges()
                .debounce(500, TimeUnit.MILLISECONDS)
                .toLatestFlowable()
        )
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .let { if (groupManga) it.toGroup() else it.toListing() }
            .addTo(disposable)*/
    }

    private fun Flowable<Triple<List<MangaModel>, MutableList<Sources>, CharSequence>>.toGroup() = map { mapManga2(it) }
        .subscribe { list ->
            (adapterType as GalleryFavoriteAdapter.GalleryGroupAdapter).setData2(list.toList())
            setNewDataUi(list.flatMap(Map.Entry<String, List<MangaModel>>::value).size)
        }

    private fun Flowable<Triple<List<MangaModel>, MutableList<Sources>, CharSequence>>.toListing() = map { mapManga(it) }
        .subscribe { list ->
            (adapterType as GalleryFavoriteAdapter.GalleryListingAdapter).setData(list)
            setNewDataUi(list.size)
        }

    private fun setNewDataUi(size: Int) {
        favorite_search_layout.hint = resources.getQuantityString(R.plurals.numFavorites, size, size)
        favoriteMangaRV.smoothScrollToPosition(0)
    }

    private fun uiSetup() {
        favoriteMangaRV.layoutManager = AutoFitGridLayoutManager(this, 360).apply { orientation = GridLayoutManager.VERTICAL }
        favoriteMangaRV.adapter = adapterType
        favoriteMangaRV.setItemViewCacheSize(20)
        favoriteMangaRV.setHasFixedSize(true)

        Sources.values().forEach {
            sourceFilter.addView(Chip(this).apply {
                text = it.name
                isCheckable = true
                isClickable = true
                isChecked = true
                setOnCheckedChangeListener { _, isChecked -> addOrRemoveSource(isChecked, it) }
                setOnLongClickListener {
                    sourceFilter.children.filterIsInstance<Chip>().forEach { it.isChecked = false }
                    isChecked = true
                    true
                }
            })
        }

    }

    private val mapManga: (Triple<List<MangaModel>, List<Sources>, CharSequence>) -> List<MangaModel> = { pair ->
        pair.first.sortedBy(MangaModel::title).filter { it.source in pair.second && it.title.contains(pair.third, true) }
    }

    private val mapManga2: (Triple<List<MangaModel>, List<Sources>, CharSequence>) -> Map<String, List<MangaModel>> = { pair ->
        pair.first
            .sortedBy(MangaModel::title)
            .filter { it.source in pair.second && it.title.contains(pair.third, true) }
            .groupByCondition(MangaModel::title) { s, name -> s.title.similarity(name.title) >= .8f }
    }

    private fun addOrRemoveSource(isChecked: Boolean, sources: Sources) {
        sourcesList = sourcesList?.apply { if (isChecked) add(sources) else remove(sources) }
    }

    override fun onDestroy() {
        fireListener.listener?.remove()
        FirebaseDb.detachListener()
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

fun DragSwipeAdapter<Pair<String, List<MangaModel>>, *>.setData2(newList: List<Pair<String, List<MangaModel>>>) {
    val diffCallback = object : DragSwipeDiffUtil<Pair<String, List<MangaModel>>>(dataList, newList) {
        override fun areContentsTheSame(oldItem: Pair<String, List<MangaModel>>, newItem: Pair<String, List<MangaModel>>): Boolean =
            oldItem.first == newItem.first

        override fun areItemsTheSame(oldItem: Pair<String, List<MangaModel>>, newItem: Pair<String, List<MangaModel>>): Boolean =
            oldItem.first === newItem.first
    }
    val diffResult = DiffUtil.calculateDiff(diffCallback)
    dataList.clear()
    dataList.addAll(newList)
    diffResult.dispatchUpdatesTo(this)
}
