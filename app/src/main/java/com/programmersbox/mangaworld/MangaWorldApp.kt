package com.programmersbox.mangaworld

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.widget.Toast
import com.facebook.stetho.Stetho
import com.github.piasy.biv.BigImageViewer
import com.github.piasy.biv.loader.glide.GlideImageLoader
import com.programmersbox.helpfulutils.*
import com.programmersbox.loggingutils.Loged
import com.programmersbox.mangaworld.utils.MangaInfoCache
import io.reactivex.plugins.RxJavaPlugins
import java.util.*

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
            createNotificationChannel("appUpdate", importance = NotificationChannelImportance.HIGH)
        }

        BigImageViewer.initialize(GlideImageLoader.with(this))

        RxJavaPlugins.setErrorHandler {
            it.printStackTrace()
            try {
                runOnUIThread { Toast.makeText(this, it.cause?.localizedMessage, Toast.LENGTH_SHORT).show() }
            } catch (e: Exception) {
            }
        }
        setAlarmUp()
    }

    private fun setAlarmUp() {
        val updateCheckIntent = Intent(this, UpdateReceiver::class.java)
        val code = 3
        if (!AlarmUtils.hasAlarm(this, updateCheckIntent, code)) {
            val pendingIntent = PendingIntent.getBroadcast(this, code, updateCheckIntent, 0)
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = System.currentTimeMillis()
            val timeToSet = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) timeToNextHourOrHalf() else 1_800_000
            val firstMillis = calendar.timeInMillis + timeToSet
            //alarmManager.cancel(pendingIntent)
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                firstMillis,
                AlarmManager.INTERVAL_HOUR,
                pendingIntent
            )
        }

        //val work = WorkManager.getInstance(this)

        //work.cancelAllWork()

        /*try {

            val work = WorkManager.getInstance(this)

            work.cancelAllWork()

            work.enqueueUniquePeriodicWork(
                "updateChecks",
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequest.Builder(UpdateWorker::class.java, 1, TimeUnit.HOURS, 15, TimeUnit.MINUTES)
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .setRequiresBatteryNotLow(false)
                            .setRequiresCharging(false)
                            .setRequiresDeviceIdle(false)
                            .setRequiresStorageNotLow(false)
                            .build()
                    )
                    .also {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            it.setInitialDelay(timeToNextHourOrHalf(), TimeUnit.MILLISECONDS)
                        } else {
                            it.setInitialDelay(10, TimeUnit.SECONDS)
                        }
                    }
                    .build()
            ).state.observeForever {
                println("The state is $it")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }*/
    }

}
