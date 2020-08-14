package com.programmersbox.mangaworld

import android.app.Service
import android.content.Context
import android.os.Build
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import androidx.work.*
import com.programmersbox.helpfulutils.activityManager

@RequiresApi(Build.VERSION_CODES.N)
class MangaWorldTileService : TileService() {
    override fun onClick() {
        super.onClick()
        /*if(!isMyServiceRunning(UpdateCheckService::class.java)) {
            val showCheck = Intent(this@MangaWorldTileService, UpdateCheckService::class.java)
            startService(showCheck)
        }*/

        //Loged.f(isMyServiceRunning<UpdateCheckService>())

        startCheck()
        //isRunning?.let { stopService(it) }
    }

    private fun startCheck() {
        WorkManager.getInstance(this).enqueueUniqueWork(
            "manualUpdateCheck",
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<UpdateWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .setRequiresBatteryNotLow(false)
                        .setRequiresCharging(false)
                        .setRequiresDeviceIdle(false)
                        .setRequiresStorageNotLow(false)
                        .build()
                )
                .build()
        )
        /*val showCheck = Intent(this@MangaWorldTileService, UpdateCheckService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(showCheck)
        } else {
            startService(showCheck)
        }*/
    }

    inline fun <reified T : Service> Context.isMyServiceRunning(): Boolean =
        activityManager.getRunningServices(Integer.MAX_VALUE).any { it.service.className == T::class.java.name }
}