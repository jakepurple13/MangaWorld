package com.programmersbox.mangaworld

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.*
import com.facebook.stetho.Stetho
import com.github.piasy.biv.BigImageViewer
import com.github.piasy.biv.loader.glide.GlideImageLoader
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.programmersbox.helpfulutils.*
import com.programmersbox.loggingutils.Loged
import com.programmersbox.loggingutils.f
import com.programmersbox.mangaworld.utils.MangaInfoCache
import com.programmersbox.mangaworld.utils.useUpdate
import com.tonyodev.fetch2.DefaultFetchNotificationManager
import com.tonyodev.fetch2.Fetch
import com.tonyodev.fetch2.FetchConfiguration
import com.tonyodev.fetch2okhttp.OkHttpDownloader
import io.reactivex.plugins.RxJavaPlugins
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

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
            createNotificationGroup("random10")
            createNotificationChannel("updateCheckChannel", importance = NotificationChannelImportance.MIN)
            createNotificationChannel("appUpdate", importance = NotificationChannelImportance.HIGH)
        }

        BigImageViewer.initialize(GlideImageLoader.with(this))

        val okHttpClient: OkHttpClient = OkHttpClient.Builder().build()

        val fetchConfiguration: FetchConfiguration = FetchConfiguration.Builder(this)
            .setDownloadConcurrentLimit(10)
            .setHttpDownloader(OkHttpDownloader(okHttpClient))
            .setNotificationManager(object : DefaultFetchNotificationManager(this) {
                override fun getFetchInstanceForNamespace(namespace: String): Fetch = Fetch.getDefaultInstance()
            })
            .build()

        Fetch.setDefaultInstanceConfiguration(fetchConfiguration)

        RxJavaPlugins.setErrorHandler {
            it.printStackTrace()
            FirebaseCrashlytics.getInstance().recordException(it)
            try {
                //runOnUIThread { Toast.makeText(this, it.cause?.localizedMessage, Toast.LENGTH_SHORT).show() }
            } catch (e: Exception) {
                FirebaseCrashlytics.getInstance().recordException(e)
            }
        }
        setAlarmUp()

        MobileAds.initialize(this) { s -> Loged.f(s.adapterStatusMap.entries.joinToString { "${it.key}=(${it.value.initializationState}, ${it.value.description})" }) }
        MobileAds.setRequestConfiguration(RequestConfiguration.Builder().setTestDeviceIds(listOf("BCF3E346AED658CDCCB1DDAEE8D84845")).build())
    }

    private fun setAlarmUp() {
        val updateCheckIntent = Intent(this, UpdateReceiver::class.java)
        val code = 3
        AlarmUtils.cancelAlarm(this, updateCheckIntent, code)
        /*if (!AlarmUtils.hasAlarm(this, updateCheckIntent, code)) {
            val pendingIntent = PendingIntent.getBroadcast(this, code, updateCheckIntent, 0)
            val timeToSet = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) timeToNextHourOrHalf() else timeToNext(1_800_000)
            val firstMillis = System.currentTimeMillis() + timeToSet
            //alarmManager.cancel(pendingIntent)
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                firstMillis,
                AlarmManager.INTERVAL_HOUR,
                pendingIntent
            )
        }*/

        //val work = WorkManager.getInstance(this)

        //work.cancelAllWork()

        setupUpdate(this, useUpdate)

    }

    companion object {
        fun setupUpdate(context: Context, shouldCheck: Boolean) {
            try {

                val work = WorkManager.getInstance(context)

                //work.cancelAllWork()

                if (shouldCheck) {
                    work.enqueueUniquePeriodicWork(
                        "updateChecks",
                        ExistingPeriodicWorkPolicy.KEEP,
                        PeriodicWorkRequest.Builder(UpdateWorker::class.java, 1, TimeUnit.HOURS)
                            //PeriodicWorkRequest.Builder(UpdateWorker::class.java, 15, TimeUnit.MINUTES)
                            .setConstraints(
                                Constraints.Builder()
                                    .setRequiredNetworkType(NetworkType.CONNECTED)
                                    .setRequiresBatteryNotLow(false)
                                    .setRequiresCharging(false)
                                    .setRequiresDeviceIdle(false)
                                    .setRequiresStorageNotLow(false)
                                    .build()
                            )
                            .setInitialDelay(10, TimeUnit.SECONDS)
                            .build()
                    ).state.observeForever { println(it) }
                } else work.cancelAllWork()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

}
