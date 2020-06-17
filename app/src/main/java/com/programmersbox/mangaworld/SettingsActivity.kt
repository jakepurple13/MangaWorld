package com.programmersbox.mangaworld

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.googlematerial.GoogleMaterial
import com.mikepenz.iconics.utils.colorInt
import com.mikepenz.iconics.utils.sizeDp
import com.programmersbox.manga_db.MangaDatabase
import com.programmersbox.mangaworld.utils.*
import com.programmersbox.rxutils.invoke
import de.Maxr1998.modernpreferences.PreferencesAdapter
import de.Maxr1998.modernpreferences.helpers.*
import de.Maxr1998.modernpreferences.preferences.SeekBarPreference
import de.Maxr1998.modernpreferences.preferences.TwoStatePreference
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class SettingsActivity : AppCompatActivity() {

    private lateinit var preferenceView: RecyclerView
    private val preferencesAdapter = PreferencesAdapter()
    private val disposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferenceView = RecyclerView(this)
        setContentView(preferenceView)
        preferenceView.layoutManager = LinearLayoutManager(this)
        preferenceView.adapter = preferencesAdapter

        preferencesAdapter.setRootScreen(createRootScreen())
        //preferencesAdapter.onScreenChangeListener = this
        // Restore adapter state from saved state
        savedInstanceState?.getParcelable<PreferencesAdapter.SavedState>("adapter")?.let(preferencesAdapter::loadSavedState)
    }

    private fun createRootScreen() = screen(this) {
        categoryHeader("settingsHeader") { title = "Settings" }

        switch("stayOnAdult") {
            title = "Leave app while adult manga visible"
            summary = "When you leave the app, we can choose a different source if you are on a source that's for adults"
            iconRes = android.R.drawable.sym_def_app_icon
            defaultValue = stayOnAdult
            checkedChangeListener = object : TwoStatePreference.OnCheckedChangeListener {
                override fun onCheckedChanged(preference: TwoStatePreference, holder: PreferencesAdapter.ViewHolder?, checked: Boolean): Boolean {
                    stayOnAdult = checked
                    return true
                }
            }
        }

        switch("groupManga") {
            title = "Group Favorite Manga by Title"
            summary = "If true, favorites will be grouped together by title"
            iconRes = android.R.drawable.sym_def_app_icon
            defaultValue = groupManga
            checkedChangeListener = object : TwoStatePreference.OnCheckedChangeListener {
                override fun onCheckedChanged(preference: TwoStatePreference, holder: PreferencesAdapter.ViewHolder?, checked: Boolean): Boolean {
                    groupManga = checked
                    return true
                }
            }
        }

        switch("usePalette") {
            title = "Use Palette"
            iconRes = android.R.drawable.sym_def_app_icon
            defaultValue = usePalette
            checkedChangeListener = object : TwoStatePreference.OnCheckedChangeListener {
                override fun onCheckedChanged(preference: TwoStatePreference, holder: PreferencesAdapter.ViewHolder?, checked: Boolean): Boolean {
                    usePalette = checked
                    return true
                }
            }
        }

        val publisher = PublishSubject.create<Int>()

        publisher
            .debounce(1000L, TimeUnit.MILLISECONDS)
            .subscribe(this@SettingsActivity::changeChapterHistorySize)
            .addTo(disposable)

        seekBar("chapterHistorySize") {
            title = "Chapter History Cache"
            summary = "Set how many chapters should be saved in history"
            iconRes = android.R.drawable.ic_menu_recent_history
            default = chapterHistorySize
            max = 100
            min = 0
            step = 1
            seekListener = object : SeekBarPreference.OnSeekListener {
                override fun onSeek(preference: SeekBarPreference, holder: PreferencesAdapter.ViewHolder?, value: Int): Boolean {
                    publisher(value)
                    return true
                }
            }
        }

        seekBar("batteryAlertPercent") {
            title = "Battery Alert Percent"
            summary = "Set when you want the battery icon too turn red while reading"
            icon = IconicsDrawable(this@SettingsActivity, GoogleMaterial.Icon.gmd_battery_alert).apply {
                colorInt = Color.RED
                sizeDp = 24
            }
            default = batteryAlertPercent.roundToInt()
            max = 100
            min = 1
            step = 1
            seekListener = object : SeekBarPreference.OnSeekListener {
                override fun onSeek(preference: SeekBarPreference, holder: PreferencesAdapter.ViewHolder?, value: Int): Boolean {
                    batteryAlertPercent = value.toFloat()
                    return true
                }
            }
        }

        switch("useCache") {
            title = "Use Cache"
            defaultValue = useCache
            checkedChangeListener = object : TwoStatePreference.OnCheckedChangeListener {
                override fun onCheckedChanged(preference: TwoStatePreference, holder: PreferencesAdapter.ViewHolder?, checked: Boolean): Boolean {
                    useCache = checked
                    return true
                }
            }
        }

        seekBar("cacheSize") {
            dependency = "useCache"
            title = "Cache Size"
            min = 1
            max = 100
            step = 1
            default = cacheSize
            onSeek { _, _, i ->
                cacheSize = i
                true
            }
        }

        checkBox("sync") {
            title = "Sync"
            onClicked {
                GlobalScope.launch { FirebaseDb.uploadAllItems(MangaDatabase.getInstance(this@SettingsActivity).mangaDao()) }
                true
            }
        }

    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save the adapter state as a parcelable into the Android-managed instance state
        outState.putParcelable("adapter", preferencesAdapter.getSavedState())
    }

    override fun onDestroy() {
        disposable.dispose()
        super.onDestroy()
    }
}