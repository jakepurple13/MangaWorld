package com.programmersbox.mangaworld

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.programmersbox.gsonutils.putExtra
import com.programmersbox.manga_sources.mangasources.Sources
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class DeepLinkActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_deep_link)

        val action: String? = intent?.action
        val data: Uri? = intent?.data

        println(action)
        println(data)
        println(data.toString())

        GlobalScope.launch {
            val manga = Sources.getSourceByUrl(data.toString())?.getMangaModelByUrl(data.toString())
            println(manga)
            runOnUiThread {
                startActivity(Intent(this@DeepLinkActivity, MangaActivity::class.java).apply { putExtra("manga", manga) })
                finish()
            }
        }

    }
}