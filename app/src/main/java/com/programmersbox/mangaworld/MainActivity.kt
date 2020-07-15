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
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.ktx.Firebase
import com.jakewharton.rxbinding2.widget.textChanges
import com.leinardi.android.speeddial.SpeedDialActionItem
import com.programmersbox.gsonutils.sharedPrefNotNullObjectDelegate
import com.programmersbox.helpfulutils.requestPermissions
import com.programmersbox.helpfulutils.setEnumSingleChoiceItems
import com.programmersbox.manga_db.MangaDatabase
import com.programmersbox.manga_sources.mangasources.MangaModel
import com.programmersbox.manga_sources.mangasources.Sources
import com.programmersbox.mangaworld.adapters.GalleryListAdapter
import com.programmersbox.mangaworld.adapters.MangaListAdapter
import com.programmersbox.mangaworld.utils.*
import com.programmersbox.mangaworld.views.AutoFitGridLayoutManager
import com.programmersbox.mangaworld.views.EndlessScrollingListener
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Flowables
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
    private var mangaViewType: MangaListView by sharedPrefNotNullObjectDelegate(MangaListView.values().random())
    private val firebaseAuthentication = FirebaseAuthentication(this, this)

    override fun onStart() {
        super.onStart()
        firebaseAuthentication.onStart()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        firebaseAuthentication.onActivityResult(requestCode, resultCode, data)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        firebaseAuthentication.authenticate()

        requestPermissions(Manifest.permission.INTERNET) {
            if (!it.isGranted) Toast.makeText(this, "Need ${it.deniedPermissions} to work", Toast.LENGTH_SHORT).show()
        }

        rvSetup()
        menuSetup()
        searchSetup()

        GlobalScope.launch { AppUpdateChecker(this@MainActivity).checkForUpdate() }

        //DownloadedManga().loadDownloaded()

    }

    private fun menuSetup() {
        val signDial = SpeedDialActionItem.Builder(R.id.signInId, R.drawable.common_google_signin_btn_icon_dark)
            .setLabel(if (FirebaseAuthentication.currentUser != null) "Sign out" else "Sign in")

        menuOptions.inflate(R.menu.main_menu_items)
        menuOptions.addActionItem(signDial.create())
        firebaseAuthentication.auth.addAuthStateListener {
            menuOptions.replaceActionItem(
                signDial.create(),
                signDial.setLabel(if (FirebaseAuthentication.currentUser != null) "Sign out" else "Sign in").create()
            )
        }
        menuOptions.setOnActionSelectedListener {
            when (it.id) {
                R.id.signInId -> {
                    if (FirebaseAuthentication.currentUser != null)
                        MaterialAlertDialogBuilder(this)
                            .setTitle("Are you sure you want to sign out?")
                            .setPositiveButton("Yes") { d, _ ->
                                firebaseAuthentication.signOut()
                                d.dismiss()
                            }
                            .setNegativeButton("No") { d, _ -> d.dismiss() }
                            .show()
                    else firebaseAuthentication.signIn()
                    //menuOptions.replaceActionItem(signDial, signDial)
                    //menuOptions.close() // To close the Speed Dial with animation
                    //return@setOnActionSelectedListener true // false will close it without animation
                }
                R.id.viewFavoritesMenu -> {
                    startActivity(Intent(this, FavoriteActivity::class.java))
                    //menuOptions.close() // To close the Speed Dial with animation
                    //return@setOnActionSelectedListener true // false will close it without animation
                }
                R.id.viewSettingsMenu -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    //menuOptions.close() // To close the Speed Dial with animation
                    //return@setOnActionSelectedListener true // false will close it without animation
                }
                R.id.viewHistoryMenu -> {
                    startActivity(Intent(this, HistoryActivity::class.java))
                    //menuOptions.close() // To close the Speed Dial with animation
                    //return@setOnActionSelectedListener true // false will close it without animation
                }
                R.id.changeSourcesMenu -> {
                    MaterialAlertDialogBuilder(this)
                        .setTitle(getText(R.string.chooseASource))
                        .setEnumSingleChoiceItems(Sources.values().map(Sources::name).toTypedArray(), currentSource) { source, dialog ->
                            if (currentSource != source) {
                                currentSource = source
                                search_layout.hint = getString(R.string.searchHint, currentSource.name)
                                reset()
                                dialog.dismiss()
                            }
                        }
                        .show()
                    //menuOptions.close() // To close the Speed Dial with animation
                    //return@setOnActionSelectedListener true // false will close it without animation
                }
                R.id.checkForUpdates -> {
                    val showCheck = Intent(this, UpdateCheckService::class.java)
                    startService(showCheck)
                    //menuOptions.close()
                    //return@setOnActionSelectedListener true // false will close it without animation
                }
            }
            menuOptions.close()
            true
        }
    }

    private fun loadNewManga() {
        refresh.isRefreshing = true
        GlobalScope.launch {
            try {
                val list = currentSource.getManga(pageNumber++).toList()
                mangaList.addAll(list)
                runOnUiThread {
                    adapter.addItems(list)
                    adapter2.addItems(list)
                    search_layout.suffixText = "${mangaList.size}"
                }
            } catch (e: Exception) {
                FirebaseCrashlytics.getInstance().log("$currentSource had an error")
                FirebaseCrashlytics.getInstance().recordException(e)
                Firebase.analytics.logEvent("manga_load_error") {
                    param(FirebaseAnalytics.Param.ITEM_NAME, currentSource.name)
                }
                runOnUiThread {
                    MaterialAlertDialogBuilder(this@MainActivity)
                        .setTitle("Something Went Wrong!")
                        .setMessage("Something happened while trying to load $currentSource. Please try again or use a different source, but please report this to the developer.")
                        .setPositiveButton("OK") { d, _ -> d.dismiss() }
                        .show()
                }
            } finally {
                runOnUiThread { refresh.isRefreshing = false }
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

    private val listener: FirebaseDb.FirebaseListener = FirebaseDb.FirebaseListener()

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

        Flowables.combineLatest(
            MangaDatabase.getInstance(this).mangaDao().getAllManga(),
            listener.getAllMangaFlowable()
        ) { db, fire -> (db + fire).distinctBy { it.mangaUrl }.map { it.toMangaModel() } }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                adapter2.favoriteLoad(it)
                adapter.favoriteLoad(it)
            }
            .addTo(disposable)
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
        listener.listener?.remove()
        //FirebaseDb.detachListener()
        if (!stayOnAdult && currentSource.isAdult) currentSource = Sources.values().filterNot(Sources::isAdult).random()
        disposable.dispose()
        super.onDestroy()
    }

}