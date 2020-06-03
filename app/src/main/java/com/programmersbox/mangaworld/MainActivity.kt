package com.programmersbox.mangaworld

import android.Manifest
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.LottieDrawable
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val disposable = CompositeDisposable()
    private val mangaList = mutableListOf<MangaModel>()
    private val adapter = MangaListAdapter(this, disposable)
    private val adapter2 = GalleryListAdapter(this, disposable)
    private var pageNumber = 1
    private var mangaViewType: MangaListView by sharedPrefNotNullObjectDelegate(MangaListView.LINEAR)
    private val lottie = LottieDrawable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestPermissions(Manifest.permission.INTERNET) {
            if (!it.isGranted) Toast.makeText(this, "Need ${it.deniedPermissions} to work", Toast.LENGTH_SHORT).show()
        }

        searchSetup()
        rvSetup()
        menuSetup()
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
            val list = currentSource().getManga(pageNumber++).toList()
            mangaList.addAll(list)
            runOnUiThread {
                adapter.addItems(list)
                adapter2.addItems(list)
                refresh.isRefreshing = false
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
        lottie.check(mangaListView == MangaListView.LINEAR)
        when (mangaListView) {
            MangaListView.LINEAR -> listView()
            MangaListView.GRID -> galleryView()
        }
    }

    private fun LottieDrawable.check(checked: Boolean) {
        val endProgress = if (checked) 1f else 0f
        val animator = ValueAnimator.ofFloat(progress, endProgress)
            .apply { addUpdateListener { animation: ValueAnimator -> progress = animation.animatedValue as Float } }
        animator.start()
    }

    private fun rvSetup() {

        LottieCompositionFactory.fromRawRes(this, R.raw.list_to_grid).addListener {
            lottie.composition = it
            changeViewType.setImageDrawable(lottie)
        }

        setMangaView(mangaViewType)

        changeViewType.setOnClickListener {
            setMangaView(
                when (mangaViewType) {
                    MangaListView.LINEAR -> MangaListView.GRID
                    MangaListView.GRID -> MangaListView.LINEAR
                }
            )
        }

        loadNewManga()

        mangaRV.addOnScrollListener(object : EndlessScrollingListener(mangaRV.layoutManager!!) {
            override fun onLoadMore(page: Int, totalItemsCount: Int, view: RecyclerView?) {
                if (currentSource().hasMorePages && search_info.text.isNullOrEmpty()) loadNewManga()
            }
        })

        refresh.setOnRefreshListener { reset() }
    }

    private fun reset() {
        mangaList.clear()
        adapter.setListNotify(mangaList)
        adapter2.setListNotify(mangaList)
        pageNumber = 1
        loadNewManga()
    }

    private fun searchSetup() {
        search_layout.hint = getString(R.string.searchHint, currentSource.name)
        search_info.doOnTextChanged { text, _, _, _ -> adapter.setListNotify(currentSource().searchManga(text.toString(), pageNumber, mangaList)) }
        /*val searching = MutableStateFlow<CharSequence>("")
        searching
            .debounce(500)
            .map { currentSource!!().searchManga(it, pageNumber, mangaList) }
            .collectOnUi { adapter.setListNotify(it) }
        search_info.doOnTextChanged { text, _, _, _ -> searching(text!!) }*/
    }

    override fun onDestroy() {
        if (!stayOnAdult && currentSource.isAdult) currentSource = Sources.values().filterNot(Sources::isAdult).random()
        disposable.dispose()
        super.onDestroy()
    }

}