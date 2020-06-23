package com.programmersbox.mangaworld

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.programmersbox.gsonutils.getObjectExtra
import com.programmersbox.loggingutils.Loged
import com.programmersbox.loggingutils.f
import com.programmersbox.manga_sources.mangasources.MangaModel
import com.programmersbox.mangaworld.adapters.BubbleListAdapter
import kotlinx.android.synthetic.main.activity_bubble.*

class BubbleActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bubble)
        val items = intent.getObjectExtra<List<MangaModel>>("mangaList").orEmpty()
        Loged.f(items.map { it.title })
        bubbleRV.adapter = BubbleListAdapter(this).apply { addItems(items) }
    }
}