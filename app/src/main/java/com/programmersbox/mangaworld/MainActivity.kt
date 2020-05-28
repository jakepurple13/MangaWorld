package com.programmersbox.mangaworld

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.programmersbox.helpfulutils.requestPermissions
import com.programmersbox.helpfulutils.setEnumSingleChoiceItems
import com.programmersbox.manga_sources.mangasources.MangaModel
import com.programmersbox.manga_sources.mangasources.Sources
import com.programmersbox.mangaworld.adapters.MangaListAdapter
import com.programmersbox.mangaworld.utils.currentSource
import com.programmersbox.mangaworld.views.EndlessScrollingListener
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val mangaList = mutableListOf<MangaModel>()
    private val adapter = MangaListAdapter(this)
    private var pageNumber = 1

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
                        .setTitle("Choose a Source")
                        .setEnumSingleChoiceItems(Sources.values().map(Sources::name).toTypedArray(), currentSource) { source, dialog ->
                            currentSource = source
                            search_layout.hint = "Search ${currentSource.name}"
                            reset()
                            dialog.dismiss()
                        }
                        .show()
                    menuOptions.close() // To close the Speed Dial with animation
                    return@setOnActionSelectedListener true // false will close it without animation
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
                refresh.isRefreshing = false
            }
        }
    }

    private fun rvSetup() {
        mangaRV.adapter = adapter
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
        pageNumber = 1
        loadNewManga()
    }

    private fun searchSetup() {
        search_layout.hint = "Search ${currentSource.name}"
        search_info.doOnTextChanged { text, _, _, _ -> adapter.setListNotify(currentSource().searchManga(text.toString(), pageNumber, mangaList)) }
        /*val searching = MutableStateFlow<CharSequence>("")
        searching
            .debounce(500)
            .map { currentSource!!().searchManga(it, pageNumber, mangaList) }
            .collectOnUi { adapter.setListNotify(it) }
        search_info.doOnTextChanged { text, _, _, _ -> searching(text!!) }*/
    }

}