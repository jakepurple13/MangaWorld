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
        sourceChangeSetup()
        rvSetup()
        viewFavorites.setOnClickListener { startActivity(Intent(this, FavoriteActivity::class.java)) }
    }

    private fun loadNewManga() {
        GlobalScope.launch {
            mangaList.addAll(currentSource!!().getManga(pageNumber++))
            runOnUiThread { adapter.addItems(mangaList) }
        }
    }

    private fun rvSetup() {
        mangaRV.adapter = adapter
        loadNewManga()

        mangaRV.addOnScrollListener(object : EndlessScrollingListener(mangaRV.layoutManager!!) {
            override fun onLoadMore(page: Int, totalItemsCount: Int, view: RecyclerView?) {
                if (currentSource!!().hasMorePages && search_info.text.isNullOrEmpty()) loadNewManga()
            }
        })
    }

    private fun sourceChangeSetup() {
        changeSources.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Choose a Source")
                .setEnumSingleChoiceItems(Sources.values().map(Sources::name).toTypedArray(), currentSource!!) { source, dialog ->
                    mangaList.clear()
                    adapter.setListNotify(mangaList)
                    currentSource = source
                    search_layout.hint = "Search ${currentSource!!.name}"
                    pageNumber = 1
                    loadNewManga()
                    dialog.dismiss()
                }
                .show()
        }
    }

    private fun searchSetup() {
        search_layout.hint = "Search ${currentSource!!.name}"
        search_info.doOnTextChanged { text, _, _, _ -> adapter.setListNotify(currentSource!!().searchManga(text.toString(), pageNumber, mangaList)) }
        /*val searching = MutableStateFlow("")
        searching
            .debounce(500)
            .map { source().searchManga(it, pageNumber, mangaList) }
            .collectOnUi { adapter.setListNotify(it) }*/
        //search_info.doOnTextChanged { text, _, _, _ -> searching(text.toString()) }
    }

}