package com.programmersbox.mangaworld

import android.content.Intent
import android.os.Build
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.N)
class MangaWorldTileService : TileService() {
    override fun onClick() {
        super.onClick()
        val showCheck = Intent(this@MangaWorldTileService, UpdateCheckService::class.java)
        startService(showCheck)
    }
}