package com.programmersbox.mangaworld

import android.app.IntentService
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import androidx.core.app.TaskStackBuilder
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.programmersbox.gsonutils.putExtra
import com.programmersbox.helpfulutils.*
import com.programmersbox.loggingutils.Loged
import com.programmersbox.loggingutils.f
import com.programmersbox.manga_db.MangaDatabase
import com.programmersbox.manga_sources.mangasources.Sources
import com.programmersbox.mangaworld.utils.FirebaseDb
import com.programmersbox.mangaworld.utils.canBubble
import com.programmersbox.mangaworld.utils.dbAndFireMangaSync
import com.programmersbox.mangaworld.utils.toMangaModel
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class UpdateCheckService : IntentService(UpdateCheckService::class.java.name) {

    override fun onHandleIntent(intent: Intent?) {
        startForeground(13, NotificationDslBuilder.builder(this, "updateCheckChannel", R.mipmap.ic_launcher) {
            onlyAlertOnce = true
            ongoing = true
            progress {
                this.max = 100
                this.progress = 0
                indeterminate = true
            }
            message = getText(R.string.startingUpdateCheck)
            subText = getText(R.string.checkingUpdate)
        })
        sendRunningNotification(100, 0, getText(R.string.startingUpdateCheck))
        val dao = MangaDatabase.getInstance(this@UpdateCheckService).mangaDao()
        val mangaListSize: Int
        //Sources.getUpdateSearches().flatMap { m -> m.getManga() }
        dbAndFireMangaSync(dao)
            .let {
                it.intersect(Sources.getUpdateSearches()
                    //.filter { s -> it.any { m -> m.source == s } }
                    .flatMap { m -> m.getManga() }) { o, n -> o.mangaUrl == n.mangaUrl }
            }
            .distinctBy { m -> m.mangaUrl }
            .also { mangaListSize = it.size }
            .mapIndexedNotNull { index, model ->
                sendRunningNotification(mangaListSize, index, model.title)
                try {
                    Triple(model.numChapters, model.toMangaModel().toInfoModel(), model)
                } catch (e: Exception) {
                    println(e.localizedMessage)
                    null
                }
            }
            .filter { it.first < it.second.chapters.size }
            .also {
                it.forEach { triple ->
                    val manga = triple.third
                    manga.numChapters = triple.second.chapters.size
                    dao.updateMangaById(manga).subscribe()
                    FirebaseDb.updateManga(manga).subscribe()
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
                } to it.map { m -> m.third.toMangaModel() }
            }
            .let {
                val n = notificationManager
                it.first.forEach { pair -> n.notify(pair.first, pair.second) }
                if (it.first.isNotEmpty()) n.notify(
                    42,
                    NotificationDslBuilder.builder(this@UpdateCheckService, "mangaChannel", R.mipmap.ic_launcher) {
                        title = getText(R.string.app_name)
                        subText = resources.getQuantityString(R.plurals.updateAmount, it.first.size, it.first.size)
                        groupSummary = true
                        groupAlertBehavior = GroupBehavior.ALL
                        groupId = "mangaGroup"
                        if (canBubble) {
                            addBubble {
                                bubbleIntent(
                                    PendingIntent.getActivity(
                                        this@UpdateCheckService, 0,
                                        Intent(this@UpdateCheckService, BubbleActivity::class.java).apply { putExtra("mangaList", it.second) },
                                        0
                                    )
                                )
                                desiredHeight = 600
                                icon = Icon.createWithResource(this@UpdateCheckService, R.mipmap.ic_launcher)
                            }
                            messageStyle {
                                setMainPerson {
                                    name = "MangaBot"
                                    isBot = true
                                }
                                message {
                                    message = resources.getQuantityString(R.plurals.updateAmount, it.first.size, it.first.size)
                                    setPerson {
                                        name = "MangaBot"
                                        isBot = true
                                    }
                                }
                            }
                        }
                    }
                )
                //maybe add bubble?
                sendFinishedNotification()
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

class UpdateReceiver : BroadcastReceiver() {
    override fun onReceive(p0: Context?, p1: Intent?) {
        try {
            p0?.startService(Intent(p0, UpdateCheckService::class.java))
        } catch (e: IllegalStateException) {
        }
    }
}

class UpdateWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    override fun doWork(): Result = try {
        /*startForeground(13, NotificationDslBuilder.builder(this@UpdateWorker.applicationContext, "updateCheckChannel", R.mipmap.ic_launcher) {
            onlyAlertOnce = true
            ongoing = true
            progress {
                this.max = 100
                this.progress = 0
                indeterminate = true
            }
            message = this@UpdateWorker.applicationContext.getText(R.string.startingUpdateCheck)
            subText = this@UpdateWorker.applicationContext.getText(R.string.checkingUpdate)
        })*/
        sendRunningNotification(100, 0, this@UpdateWorker.applicationContext.getText(R.string.startingUpdateCheck))
        val dao = MangaDatabase.getInstance(this@UpdateWorker.applicationContext).mangaDao()
        val mangaListSize: Int
        //dao.getAllMangaSync()
        this@UpdateWorker.applicationContext.dbAndFireMangaSync(dao)
            .let {
                it.intersect(
                    Sources.getUpdateSearches()
                        .filter { s -> it.any { m -> m.source == s } }
                        .flatMap { m -> m.getManga() }) { o, n -> o.mangaUrl == n.mangaUrl }
                    .distinctBy { m -> m.mangaUrl }
            }
            .also { mangaListSize = it.size }
            .mapIndexedNotNull { index, model ->
                sendRunningNotification(mangaListSize, index, model.title)
                try {
                    Triple(model.numChapters, model.toMangaModel().toInfoModel(), model)
                } catch (e: Exception) {
                    println(e.localizedMessage)
                    null
                }
            }
            .filter { it.first < it.second.chapters.size }
            .also {
                it.forEach { triple ->
                    val manga = triple.third
                    manga.numChapters = triple.second.chapters.size
                    dao.updateMangaById(manga).subscribe()
                    FirebaseDb.updateManga(manga).subscribe()
                }
            }
            .let {
                it.mapIndexed { index, pair ->
                    sendRunningNotification(it.size, index, pair.second.title)
                    pair.second.hashCode() to NotificationDslBuilder.builder(
                        this@UpdateWorker.applicationContext,
                        "mangaChannel",
                        R.mipmap.ic_launcher
                    ) {
                        title = pair.second.title
                        subText = pair.third.source.name
                        getBitmapFromURL(pair.second.imageUrl)?.let {
                            pictureStyle {
                                bigPicture = it
                                largeIcon = it
                                summaryText = this@UpdateWorker.applicationContext.getString(
                                    R.string.hadAnUpdate,
                                    pair.second.title,
                                    pair.second.chapters.firstOrNull()?.name ?: ""
                                )
                            }
                        } ?: bigTextStyle {
                            bigText = this@UpdateWorker.applicationContext.getString(
                                R.string.hadAnUpdate,
                                pair.second.title,
                                pair.second.chapters.firstOrNull()?.name.orEmpty()
                            )
                        }
                        groupId = "mangaGroup"
                        pendingIntent { context ->
                            TaskStackBuilder.create(context)
                                .addParentStack(MainActivity::class.java)
                                .addNextIntent(Intent(context, MangaActivity::class.java).apply { putExtra("manga", pair.third.toMangaModel()) })
                                .getPendingIntent(pair.second.hashCode(), PendingIntent.FLAG_UPDATE_CURRENT)
                        }
                    }
                } to it.map { m -> m.third.toMangaModel() }
            }
            .let {
                val n = this@UpdateWorker.applicationContext.notificationManager
                it.first.forEach { pair -> n.notify(pair.first, pair.second) }
                if (it.first.isNotEmpty()) n.notify(
                    42,
                    NotificationDslBuilder.builder(this@UpdateWorker.applicationContext, "mangaChannel", R.mipmap.ic_launcher) {
                        title = this@UpdateWorker.applicationContext.getText(R.string.app_name)
                        subText =
                            this@UpdateWorker.applicationContext.resources.getQuantityString(R.plurals.updateAmount, it.first.size, it.first.size)
                        groupSummary = true
                        groupAlertBehavior = GroupBehavior.ALL
                        groupId = "mangaGroup"
                        if (this@UpdateWorker.applicationContext.canBubble) {
                            addBubble {
                                bubbleIntent(
                                    PendingIntent.getActivity(
                                        this@UpdateWorker.applicationContext, 0,
                                        Intent(this@UpdateWorker.applicationContext, BubbleActivity::class.java)
                                            .apply { putExtra("mangaList", it.second) },
                                        0
                                    )
                                )
                                desiredHeight = 600
                                icon = Icon.createWithResource(this@UpdateWorker.applicationContext, R.mipmap.ic_launcher)
                            }
                            messageStyle {
                                setMainPerson {
                                    name = "MangaBot"
                                    isBot = true
                                }
                                message {
                                    message = this@UpdateWorker.applicationContext.resources.getQuantityString(
                                        R.plurals.updateAmount,
                                        it.first.size,
                                        it.first.size
                                    )
                                    setPerson {
                                        name = "MangaBot"
                                        isBot = true
                                    }
                                }
                            }
                        }
                    }
                )
            }
        sendFinishedNotification()
        Loged.f("Done")
        Result.success()
    } catch (e: Exception) {
        e.printStackTrace()
        Result.failure()
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
        val notification = NotificationDslBuilder.builder(this@UpdateWorker.applicationContext, "updateCheckChannel", R.mipmap.ic_launcher) {
            onlyAlertOnce = true
            ongoing = true
            progress {
                this.max = max
                this.progress = progress
                indeterminate = progress == 0
            }
            message = contextText
            subText = this@UpdateWorker.applicationContext.getText(R.string.checkingUpdate)
        }
        this@UpdateWorker.applicationContext.notificationManager.notify(13, notification)
        Loged.f("Checking for $contextText")
    }

    private fun sendFinishedNotification() {
        val notification = NotificationDslBuilder.builder(this@UpdateWorker.applicationContext, "updateCheckChannel", R.mipmap.ic_launcher) {
            onlyAlertOnce = true
            subText = this@UpdateWorker.applicationContext.getText(R.string.finishedChecking)
            timeoutAfter = 750L
        }
        this@UpdateWorker.applicationContext.notificationManager.notify(13, notification)
    }
}