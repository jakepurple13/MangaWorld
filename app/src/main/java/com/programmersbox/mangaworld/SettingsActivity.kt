package com.programmersbox.mangaworld

import android.app.PendingIntent
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Icon
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mikepenz.aboutlibraries.LibsBuilder
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.googlematerial.GoogleMaterial
import com.mikepenz.iconics.utils.colorInt
import com.mikepenz.iconics.utils.sizeDp
import com.programmersbox.gsonutils.fromJson
import com.programmersbox.gsonutils.putExtra
import com.programmersbox.helpfulutils.GroupBehavior
import com.programmersbox.helpfulutils.NotificationDslBuilder
import com.programmersbox.helpfulutils.notificationManager
import com.programmersbox.helpfulutils.sizedListOf
import com.programmersbox.manga_db.MangaDatabase
import com.programmersbox.manga_sources.mangasources.MangaContext
import com.programmersbox.mangaworld.utils.*
import com.programmersbox.rxutils.invoke
import de.Maxr1998.modernpreferences.Preference
import de.Maxr1998.modernpreferences.PreferencesAdapter
import de.Maxr1998.modernpreferences.helpers.*
import de.Maxr1998.modernpreferences.preferences.SeekBarPreference
import de.Maxr1998.modernpreferences.preferences.TwoStatePreference
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
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

        pref("appInfo") {
            title = "Version: ${packageManager.getPackageInfo(packageName, 0).versionName}"
            iconRes = R.drawable.manga_world_round_logo
            clickListener = object : Preference.OnClickListener {
                @Suppress("BlockingMethodInNonBlockingContext")
                override fun onClick(preference: Preference, holder: PreferencesAdapter.ViewHolder): Boolean {
                    GlobalScope.launch {
                        val info = try {
                            withContext(Dispatchers.Default) {
                                URL("https://raw.githubusercontent.com/jakepurple13/MangaWorld/master/app/src/main/res/raw/update_changelog.json").readText()
                            }
                        } catch (e: Exception) {
                            resources.openRawResource(R.raw.update_changelog).bufferedReader().readText()
                        }.fromJson<AppInfo>()!!
                        runOnUiThread {
                            MaterialAlertDialogBuilder(this@SettingsActivity)
                                .setTitle("Update notes for ${info.version}")
                                .setItems(info.releaseNotes.toTypedArray(), null)
                                .setPositiveButton("Ok") { d, _ -> d.dismiss() }
                                .setNeutralButton("View Libraries Used") { d, _ ->
                                    d.dismiss()
                                    LibsBuilder().start(this@SettingsActivity)
                                }
                                .show()
                        }

                    }
                    return true
                }
            }
        }

        pref("accountInfo") {
            val user = FirebaseAuthentication.currentUser
            title = "Current User: ${user?.displayName}"
            summary = "Email: ${user?.email}"
        }

        switch("stayOnAdult") {
            title = "Leave app while adult manga visible"
            summary = "When you leave the app, we can choose a different source if you are on a source that's for adults"
            //iconRes = R.drawable.manga_world_round_logo
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
            iconRes = R.drawable.ic_baseline_group_24
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
            iconRes = R.drawable.ic_baseline_palette_24
            defaultValue = usePalette
            checkedChangeListener = object : TwoStatePreference.OnCheckedChangeListener {
                override fun onCheckedChanged(preference: TwoStatePreference, holder: PreferencesAdapter.ViewHolder?, checked: Boolean): Boolean {
                    usePalette = checked
                    return true
                }
            }
        }

        switch("updateCheck") {
            title = "Allow Update Checking"
            iconRes = R.drawable.ic_baseline_update_24
            defaultValue = useUpdate
            checkedChangeListener = object : TwoStatePreference.OnCheckedChangeListener {
                override fun onCheckedChanged(preference: TwoStatePreference, holder: PreferencesAdapter.ViewHolder?, checked: Boolean): Boolean {
                    useUpdate = checked
                    MangaWorldApp.setupUpdate(this@SettingsActivity, checked)
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

        pref("clearCookies") {
            title = "Clear Cookies"
            iconRes = R.drawable.ic_baseline_cached_24
            onClicked {
                MangaContext.getInstance(this@SettingsActivity).cookieManager.removeAll()
                Toast.makeText(this@SettingsActivity, "Cleared", Toast.LENGTH_SHORT).show()
                true
            }
        }

        switch("useAgo") {
            title = "Use Time Ago"
            summary = "See how long ago a chapter was updated"
            visible = false
            defaultValue = useAgo
            checkedChangeListener = object : TwoStatePreference.OnCheckedChangeListener {
                override fun onCheckedChanged(preference: TwoStatePreference, holder: PreferencesAdapter.ViewHolder?, checked: Boolean): Boolean {
                    useAgo = checked
                    return true
                }
            }
        }

        pref("show10Random") {
            title = "Show 10 Random Favorites"
            visible = false
            onClicked {
                GlobalScope.launch {
                    val db = MangaDatabase.getInstance(this@SettingsActivity).mangaDao().getAllMangaSync()
                    val fire = FirebaseDb.FirebaseListener().getAllManga()?.requireNoNulls().orEmpty()
                    val mangaList = listOf(db, fire).flatten().distinctBy { it.mangaUrl }.map { it.toMangaModel() }
                    val manga = sizedListOf(mangaList.size.takeIf { it < 10 } ?: 10) {
                        try {
                            mangaList.random()
                        } catch (e: Exception) {
                            null
                        }
                    }.requireNoNulls().also { println(it) }

                    notificationManager.notify(
                        12,
                        NotificationDslBuilder.builder(this@SettingsActivity, "mangaChannel", R.drawable.manga_world_round_logo) {
                            title = getText(R.string.app_name)
                            subText = "Here are ${manga.size} random manga"
                            groupSummary = true
                            groupAlertBehavior = GroupBehavior.ALL
                            groupId = "random10"
                            addBubble {
                                bubbleIntent(
                                    PendingIntent.getActivity(
                                        this@SettingsActivity, 12,
                                        Intent(this@SettingsActivity, BubbleActivity::class.java).apply { putExtra("mangaList", manga) },
                                        0
                                    )
                                )
                                desiredHeight = 600
                                icon = Icon.createWithResource(this@SettingsActivity, R.drawable.manga_world_round_logo)
                                autoExpandBubble = true
                            }
                            messageStyle {
                                setMainPerson {
                                    name = "MangaBot"
                                    isBot = true
                                }
                                message {
                                    message = "Here are ${manga.size} random manga"
                                    setPerson {
                                        name = "MangaBot"
                                        isBot = true
                                    }
                                }
                            }
                        }
                    )
                }
                true
            }
        }

        switch("canBubble") {
            title = "Can Bubble"
            defaultValue = canBubble
            iconRes = R.drawable.ic_baseline_bubble_chart_24
            checkedChangeListener = object : TwoStatePreference.OnCheckedChangeListener {
                override fun onCheckedChanged(preference: TwoStatePreference, holder: PreferencesAdapter.ViewHolder?, checked: Boolean): Boolean {
                    canBubble = checked
                    return true
                }
            }
        }

        switch("useCache") {
            title = "Use Cache"
            defaultValue = useCache
            iconRes = R.drawable.ic_baseline_cached_24
            checkedChangeListener = object : TwoStatePreference.OnCheckedChangeListener {
                override fun onCheckedChanged(preference: TwoStatePreference, holder: PreferencesAdapter.ViewHolder?, checked: Boolean): Boolean {
                    useCache = checked
                    return true
                }
            }
        }

        seekBar("cacheSize") {
            dependency = "useCache"
            iconRes = R.drawable.ic_baseline_cached_24
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

        /*pref("sync2") {
            title = "Sync2"
            onClicked {
                //GlobalScope.launch { FirebaseDb.uploadAllItems(MangaDatabase.getInstance(this@SettingsActivity).mangaDao()) }
                GlobalScope.launch { FirebaseDb.uploadChapters(MangaDatabase.getInstance(this@SettingsActivity).mangaDao(), this@SettingsActivity) }
                true
            }
        }*/

        /*pref("sync") {
            title = "Sync"
            onClicked {
                //GlobalScope.launch { FirebaseDb.uploadAllItems(MangaDatabase.getInstance(this@SettingsActivity).mangaDao()) }
                GlobalScope.launch { FirebaseDb.uploadAllItems2(MangaDatabase.getInstance(this@SettingsActivity).mangaDao(), this@SettingsActivity) }
                true
            }
        }*/

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