package com.programmersbox.mangaworld

import android.app.Application
import android.os.Build
import com.programmersbox.helpfulutils.createNotificationChannel
import com.programmersbox.helpfulutils.createNotificationGroup
import com.programmersbox.helpfulutils.defaultSharedPrefName
import com.programmersbox.loggingutils.Loged

class MangaWorldApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Loged.FILTER_BY_PACKAGE_NAME = "programmersbox"
        Loged.TAG = "MangaWorld"
        defaultSharedPrefName = "mangaworld"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel("mangaChannel")
            createNotificationGroup("mangaGroup")
        }
    }
}