package com.programmersbox.mangaworld

import android.app.IntentService
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.app.TaskStackBuilder
import com.programmersbox.gsonutils.putExtra
import com.programmersbox.helpfulutils.*
import com.programmersbox.loggingutils.Loged
import com.programmersbox.loggingutils.f
import com.programmersbox.manga_db.MangaDatabase
import com.programmersbox.manga_sources.mangasources.Sources
import com.programmersbox.mangaworld.utils.dbAndFireMangaSync
import com.programmersbox.mangaworld.utils.toMangaModel
import kotlinx.coroutines.*
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class UpdateCheckService : IntentService("UpdateCheckIntentService") {

    /*private val disposable = CompositeDisposable()

    override fun onUnbind(intent: Intent?): Boolean {
        disposable.dispose()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        disposable.dispose()
        super.onDestroy()
    }*/

    override fun onHandleIntent(intent: Intent?) {
        val checker = GlobalScope.launch {
            delay(300000L)
            if (isMyServiceRunning(UpdateCheckService::class.java)) stopSelf()
        }

        sendRunningNotification(100, 0, getText(R.string.startingUpdateCheck))
        val dao = MangaDatabase.getInstance(this@UpdateCheckService).mangaDao()
        GlobalScope.launch {
            val mangaListSize: Int
            //dao.getAllMangaSync()
            dbAndFireMangaSync(dao)
                .let {
                    it.intersect(
                        Sources.getUpdateSearches()
                            .filter { s -> it.any { m -> m.source == s } }
                        .flatMap { m -> m.getManga() }) { o, n -> o.mangaUrl == n.mangaUrl }
                }
                .also { mangaListSize = it.size }
                .mapIndexedNotNull { index, model ->
                    sendRunningNotification(mangaListSize, index, model.title)
                    try {
                        withContext(Dispatchers.Default) {
                            withTimeoutOrNull(10000L) { Triple(model.numChapters, model.toMangaModel().toInfoModel(), model) }
                        }
                    } catch (e: Exception) {
                        null
                    }
                }
                .filter { it.first < it.second.chapters.size }
                .also {
                    it.forEach { triple ->
                        val manga = triple.third
                        manga.numChapters = triple.second.chapters.size
                        dao.updateMangaById(manga).subscribe()
                    }
                }
                .let {
                    it.mapIndexed { index, pair ->
                        sendRunningNotification(it.size, index, pair.second.title)
                        pair.second.hashCode() to NotificationDslBuilder.builder(this@UpdateCheckService, "mangaChannel", R.mipmap.ic_launcher) {
                            title = pair.second.title
                            subText = pair.third.source.name
                            getBitmapFromURL(pair.second.imageUrl)?.let {
                                pictureStyle {
                                    bigPicture = it
                                    largeIcon = it
                                    summaryText = getString(R.string.hadAnUpdate, pair.second.title, pair.second.chapters.firstOrNull()?.name ?: "")
                                }
                            } ?: bigTextStyle {
                                bigText = getString(R.string.hadAnUpdate, pair.second.title, pair.second.chapters.firstOrNull()?.name.orEmpty())
                            }
                            groupId = "mangaGroup"
                            pendingIntent { context ->
                                TaskStackBuilder.create(context)
                                    .addParentStack(MainActivity::class.java)
                                    .addNextIntent(Intent(context, MangaActivity::class.java).apply { putExtra("manga", pair.third.toMangaModel()) })
                                    .getPendingIntent(pair.second.hashCode(), PendingIntent.FLAG_UPDATE_CURRENT)
                            }
                        }
                    }
                }
                .let {
                    val n = notificationManager
                    it.forEach { pair -> n.notify(pair.first, pair.second) }
                    if (it.isNotEmpty()) n.notify(
                        42,
                        NotificationDslBuilder.builder(this@UpdateCheckService, "mangaChannel", R.mipmap.ic_launcher) {
                            title = getText(R.string.app_name)
                            subText = resources.getQuantityString(R.plurals.updateAmount, it.size, it.size)
                            groupSummary = true
                            groupAlertBehavior = GroupBehavior.ALL
                            groupId = "mangaGroup"
                        }
                    )
                    //maybe add bubble?
                    sendFinishedNotification()
                    checker.cancel()
                    stopSelf()
                }
        }
    }

    private fun getBitmapFromURL(strURL: String?): Bitmap? = try {
        val url = URL(strURL)
        val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
        connection.doInput = true
        connection.connect()
        BitmapFactory.decodeStream(connection.inputStream)
    } catch (e: IOException) {
        e.printStackTrace()
        null
    }

    private fun sendRunningNotification(max: Int, progress: Int, contextText: CharSequence = "") {
        val notification = NotificationDslBuilder.builder(this, "updateCheckChannel", R.mipmap.ic_launcher) {
            onlyAlertOnce = true
            ongoing = true
            progress {
                this.max = max
                this.progress = progress
                indeterminate = progress == 0
            }
            message = contextText
            subText = getText(R.string.checkingUpdate)
        }
        notificationManager.notify(13, notification)
        Loged.f("Checking for $contextText")
    }

    private fun sendFinishedNotification() {
        val notification = NotificationDslBuilder.builder(this, "updateCheckChannel", R.mipmap.ic_launcher) {
            onlyAlertOnce = true
            subText = getText(R.string.finishedChecking)
            timeoutAfter = 750L
        }
        notificationManager.notify(13, notification)
    }

    private fun isMyServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = activityManager
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

}