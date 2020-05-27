package com.programmersbox.mangaworld

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.programmersbox.mangaworld.utils.cacheSize
import com.programmersbox.mangaworld.utils.useCache
import com.programmersbox.mangaworld.utils.usePalette
import de.Maxr1998.modernpreferences.PreferencesAdapter
import de.Maxr1998.modernpreferences.helpers.onSeek
import de.Maxr1998.modernpreferences.helpers.screen
import de.Maxr1998.modernpreferences.helpers.seekBar
import de.Maxr1998.modernpreferences.helpers.switch
import de.Maxr1998.modernpreferences.preferences.TwoStatePreference

class SettingsActivity : AppCompatActivity() {

    private lateinit var preferenceView: RecyclerView
    private val preferencesAdapter = PreferencesAdapter()

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
        switch("useCache") {
            title = "Use Cache"
            iconRes = android.R.drawable.sym_def_app_icon
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

    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save the adapter state as a parcelable into the Android-managed instance state
        outState.putParcelable("adapter", preferencesAdapter.getSavedState())
    }
}