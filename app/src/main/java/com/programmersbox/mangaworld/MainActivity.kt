package com.programmersbox.mangaworld

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.programmersbox.helpfulutils.requestPermissions
import com.programmersbox.manga_sources.mangasources.Sources
import com.programmersbox.mangaworld.adapters.MangaListAdapter
import com.programmersbox.mangaworld.views.EndlessScrollingListener
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val adapter = MangaListAdapter(mutableListOf(), this)

    private var pageNumber = 1
    private var source = Sources.values().random()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestPermissions(Manifest.permission.INTERNET) {
            if (!it.isGranted) Toast.makeText(this, "Need ${it.deniedPermissions} to work", Toast.LENGTH_SHORT).show()
        }

        mangaRV.adapter = adapter
        loadNewManga()

        mangaRV.addOnScrollListener(object : EndlessScrollingListener(mangaRV.layoutManager!!) {
            override fun onLoadMore(page: Int, totalItemsCount: Int, view: RecyclerView?) {
                if (source.source().hasMorePages) loadNewManga()
            }
        })

    }

    private fun loadNewManga() {
        GlobalScope.launch {
            val items = source.source().getManga(pageNumber++)
            runOnUiThread { adapter.addItems(items) }
        }
    }

}