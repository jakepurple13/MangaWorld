package com.programmersbox.mangaworld

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.widget.Toast
import com.facebook.stetho.Stetho
import com.programmersbox.helpfulutils.*
import com.programmersbox.loggingutils.Loged
import com.programmersbox.mangaworld.utils.MangaInfoCache
import io.reactivex.plugins.RxJavaPlugins


class MangaWorldApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Stetho.initializeWithDefaults(this)
        MangaInfoCache.init(this)
        Loged.FILTER_BY_PACKAGE_NAME = "programmersbox"
        Loged.TAG = "MangaWorld"
        defaultSharedPrefName = "mangaworld"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel("mangaChannel", importance = NotificationChannelImportance.HIGH)
            createNotificationGroup("mangaGroup")
            createNotificationChannel("updateCheckChannel", importance = NotificationChannelImportance.MIN)
        }

        RxJavaPlugins.setErrorHandler { runOnUIThread { Toast.makeText(this, it.cause?.localizedMessage, Toast.LENGTH_SHORT).show() } }

        val updateCheckIntent = Intent(this, UpdateCheckService::class.java)
        val pendingIntent = PendingIntent.getService(this, 10, updateCheckIntent, 0)
        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), AlarmManager.INTERVAL_HOUR, pendingIntent)
    }
}