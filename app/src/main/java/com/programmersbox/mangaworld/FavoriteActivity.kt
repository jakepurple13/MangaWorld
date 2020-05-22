package com.programmersbox.mangaworld

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.programmersbox.manga_db.MangaDatabase
import com.programmersbox.manga_db.MangaDbModel
import com.programmersbox.mangaworld.adapters.MangaListAdapter
import com.programmersbox.mangaworld.utils.toMangaModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_favorite.*

class FavoriteActivity : AppCompatActivity() {

    private val disposable = CompositeDisposable()
    private val adapter = MangaListAdapter(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorite)

        favoriteMangaRV.adapter = adapter

        MangaDatabase.getInstance(this).mangaDao().getAllManga()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .map { it.map(MangaDbModel::toMangaModel) }
            .subscribe(adapter::setListNotify)
            .addTo(disposable)

    }
}