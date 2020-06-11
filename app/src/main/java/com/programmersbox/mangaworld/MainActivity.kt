package com.programmersbox.mangaworld

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jakewharton.rxbinding2.widget.textChanges
import com.programmersbox.gsonutils.sharedPrefNotNullObjectDelegate
import com.programmersbox.helpfulutils.requestPermissions
import com.programmersbox.helpfulutils.setEnumSingleChoiceItems
import com.programmersbox.manga_sources.mangasources.MangaModel
import com.programmersbox.manga_sources.mangasources.Sources
import com.programmersbox.mangaworld.adapters.GalleryListAdapter
import com.programmersbox.mangaworld.adapters.MangaListAdapter
import com.programmersbox.mangaworld.utils.MangaListView
import com.programmersbox.mangaworld.utils.currentSource
import com.programmersbox.mangaworld.utils.stayOnAdult
import com.programmersbox.mangaworld.views.AutoFitGridLayoutManager
import com.programmersbox.mangaworld.views.EndlessScrollingListener
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private val disposable = CompositeDisposable()
    private val mangaList = mutableListOf<MangaModel>()
    private val adapter = MangaListAdapter(this, disposable)
    private val adapter2 = GalleryListAdapter(this, disposable)
    private var pageNumber = 1
    private var mangaViewType: MangaListView by sharedPrefNotNullObjectDelegate(MangaListView.LINEAR)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestPermissions(Manifest.permission.INTERNET) {
            if (!it.isGranted) Toast.makeText(this, "Need ${it.deniedPermissions} to work", Toast.LENGTH_SHORT).show()
        }

        rvSetup()
        menuSetup()
        searchSetup()
    }

    private fun menuSetup() {
        menuOptions.inflate(R.menu.main_menu_items)
        menuOptions.setOnActionSelectedListener {
            when (it.id) {
                R.id.viewFavoritesMenu -> {
                    startActivity(Intent(this, FavoriteActivity::class.java))
                    menuOptions.close() // To close the Speed Dial with animation
                    return@setOnActionSelectedListener true // false will close it without animation
                }
                R.id.viewSettingsMenu -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    menuOptions.close() // To close the Speed Dial with animation
                    return@setOnActionSelectedListener true // false will close it without animation
                }
                R.id.viewHistoryMenu -> {
                    startActivity(Intent(this, HistoryActivity::class.java))
                    menuOptions.close() // To close the Speed Dial with animation
                    return@setOnActionSelectedListener true // false will close it without animation
                }
                R.id.changeSourcesMenu -> {
                    MaterialAlertDialogBuilder(this)
                        .setTitle(getText(R.string.chooseASource))
                        .setEnumSingleChoiceItems(Sources.values().map(Sources::name).toTypedArray(), currentSource) { source, dialog ->
                            currentSource = source
                            search_layout.hint = getString(R.string.searchHint, currentSource.name)
                            reset()
                            dialog.dismiss()
                        }
                        .show()
                    menuOptions.close() // To close the Speed Dial with animation
                    return@setOnActionSelectedListener true // false will close it without animation
                }
                R.id.checkForUpdates -> {
                    val showCheck = Intent(this, UpdateCheckService::class.java)
                    startService(showCheck)
                }
            }
            false
        }
    }

    private fun loadNewManga() {
        refresh.isRefreshing = true
        GlobalScope.launch {
            val list = currentSource.getManga(pageNumber++).toList()
            mangaList.addAll(list)
            runOnUiThread {
                adapter.addItems(list)
                adapter2.addItems(list)
                refresh.isRefreshing = false
                search_layout.suffixText = "${mangaList.size}"
            }
        }
    }

    private fun listView() {
        mangaRV.layoutManager = LinearLayoutManager(this).apply { orientation = LinearLayoutManager.VERTICAL }
        mangaRV.adapter = adapter
    }

    private fun galleryView() {
        mangaRV.layoutManager = AutoFitGridLayoutManager(this, 360).apply { orientation = GridLayoutManager.VERTICAL }
        mangaRV.adapter = adapter2
    }

    private fun setMangaView(mangaListView: MangaListView) {
        mangaViewType = mangaListView
        when (mangaListView) {
            MangaListView.LINEAR -> listView()
            MangaListView.GRID -> galleryView()
        }
    }

    private fun rvSetup() {

        setMangaView(mangaViewType)

        viewToggle.check(if (mangaViewType == MangaListView.GRID) R.id.showGalleryView else R.id.showListView)
        showGalleryView.setOnClickListener { setMangaView(MangaListView.GRID) }
        showListView.setOnClickListener { setMangaView(MangaListView.LINEAR) }

        loadNewManga()

        mangaRV.addOnScrollListener(object : EndlessScrollingListener(mangaRV.layoutManager!!) {
            override fun onLoadMore(page: Int, totalItemsCount: Int, view: RecyclerView?) {
                if (currentSource.hasMorePages && search_info.text.isNullOrEmpty()) loadNewManga()
            }
        })
        mangaRV.setHasFixedSize(true)

        refresh.setOnRefreshListener { reset() }
    }

    private fun reset() {
        mangaList.clear()
        adapter.setListNotify(mangaList)
        adapter2.setListNotify(mangaList)
        pageNumber = 1
        search_info.text?.clear()
        loadNewManga()
    }

    private fun searchSetup() {
        search_layout.hint = getString(R.string.searchHint, currentSource.name)
        search_info
            .textChanges()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .debounce(500, TimeUnit.MILLISECONDS)
            .map { currentSource.searchManga(it, 1, mangaList) }
            .subscribe {
                adapter.setData(it)
                adapter2.setData(it)
                mangaRV.smoothScrollToPosition(0)
                runOnUiThread { search_layout.suffixText = "${it.size}" }
            }
            .addTo(disposable)

        search_info
            .textChanges()
            .map(CharSequence::isEmpty)
            .subscribe(refresh::setEnabled)
            .addTo(disposable)
    }

    override fun onDestroy() {
        if (!stayOnAdult && currentSource.isAdult) currentSource = Sources.values().filterNot(Sources::isAdult).random()
        disposable.dispose()
        super.onDestroy()
    }

}