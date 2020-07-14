package com.programmersbox.mangaworld

import android.app.IntentService
import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import androidx.core.app.TaskStackBuilder
import androidx.work.RxWorker
import androidx.work.WorkerParameters
import com.google.common.util.concurrent.ListenableFuture
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.ktx.Firebase
import com.programmersbox.gsonutils.putExtra
import com.programmersbox.helpfulutils.GroupBehavior
import com.programmersbox.helpfulutils.NotificationDslBuilder
import com.programmersbox.helpfulutils.intersect
import com.programmersbox.helpfulutils.notificationManager
import com.programmersbox.loggingutils.Loged
import com.programmersbox.loggingutils.f
import com.programmersbox.manga_db.MangaDatabase
import com.programmersbox.manga_db.MangaDbModel
import com.programmersbox.manga_sources.mangasources.MangaInfoModel
import com.programmersbox.manga_sources.mangasources.MangaModel
import com.programmersbox.manga_sources.mangasources.Sources
import com.programmersbox.mangaworld.utils.*
import com.programmersbox.rxutils.invoke
import io.reactivex.Single
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class UpdateCheckService : IntentService(UpdateCheckService::class.java.name) {

    private val update by lazy { UpdateNotification(this) }

    override fun onHandleIntent(intent: Intent?) {
        //FirebaseAnalytics.getInstance(this).logEvent("Start_update_check_UpdateCheckService", null)
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
        update.sendRunningNotification(100, 0, getText(R.string.startingUpdateCheck))
        val dao = MangaDatabase.getInstance(this@UpdateCheckService).mangaDao()
        val listSize: Int
        dbAndFireMangaSync3(dao)
            .let {
                it.intersect(
                    Sources.getUpdateSearches()
                        .filter { s -> it.any { m -> m.source == s } }
                        .mapNotNull { m ->
                            try {
                                m.getManga()
                            } catch (e: Exception) {
                                FirebaseCrashlytics.getInstance().log("$m had an error")
                                FirebaseCrashlytics.getInstance().recordException(e)
                                Firebase.analytics.logEvent("manga_load_error") { param(FirebaseAnalytics.Param.ITEM_NAME, m.name) }
                                null
                            }
                        }.flatten()
                ) { o, n -> o.mangaUrl == n.mangaUrl }
            }
            .distinctBy { m -> m.mangaUrl }
            .also { listSize = it.lastIndex }
            .mapIndexedNotNull { index, model ->
                update.sendRunningNotification(listSize, index, model.title)
                try {
                    val newData = model.toMangaModel().toInfoModel()
                    if (model.numChapters >= newData.chapters.size) null
                    else Pair(newData, model)
                } catch (e: Exception) {
                    println(e.localizedMessage)
                    null
                }
            }
            .also {
                it.forEach { triple ->
                    val manga = triple.second
                    manga.numChapters = triple.first.chapters.size
                    dao.updateMangaById(manga).subscribe()
                    FirebaseDb.updateManga(manga).subscribe()
                }
            }
            .let { update.mapDbModel(it) }
            .let { update.onEnd(it) }
        update.sendFinishedNotification()
    }

}

class UpdateReceiver : BroadcastReceiver() {
    override fun onReceive(p0: Context?, p1: Intent?) {
        p0?.let { FirebaseAnalytics.getInstance(it).logEvent("Start_update_check_UpdateReceiver", null) }
        try {
            p0?.startService(Intent(p0, UpdateCheckService::class.java))
        } catch (e: IllegalStateException) {
        }
    }
}

class UpdateWorker(context: Context, workerParams: WorkerParameters) : RxWorker(context, workerParams) {

    private val update by lazy { UpdateNotification(this.applicationContext) }
    private val dao by lazy { MangaDatabase.getInstance(this@UpdateWorker.applicationContext).mangaDao() }

    override fun startWork(): ListenableFuture<Result> {
        update.sendRunningNotification(100, 0, this@UpdateWorker.applicationContext.getText(R.string.startingUpdateCheck))
        return super.startWork()
    }

    override fun createWork(): Single<Result> {
        Loged.f("Starting check here")
        return Single.create<List<MangaDbModel>> { emitter ->
            Loged.f("Start")
            val list = applicationContext.dbAndFireMangaSync2(dao)
            /*val sourceList = Sources.getUpdateSearches()
                .filter { s -> list.any { m -> m.source == s } }
                .flatMap { m -> m.getManga() }*/
            val newList = list.intersect(
                Sources.getUpdateSearches()
                    .filter { s -> list.any { m -> m.source == s } }
                    .mapNotNull { m ->
                        try {
                            m.getManga()
                        } catch (e: Exception) {
                            FirebaseCrashlytics.getInstance().log("$m had an error")
                            FirebaseCrashlytics.getInstance().recordException(e)
                            Firebase.analytics.logEvent("manga_load_error") { param(FirebaseAnalytics.Param.ITEM_NAME, m.name) }
                            null
                        }
                    }.flatten()
            ) { o, n -> o.mangaUrl == n.mangaUrl }
            //emitter(list.filter { m -> sourceList.any { it.mangaUrl == m.mangaUrl } })
            emitter(newList.distinctBy { it.mangaUrl })
        }
            .map { list ->
                Loged.f("Map1")
                list.mapIndexedNotNull { index, model ->
                    update.sendRunningNotification(list.size, index, model.title)
                    try {
                        val newData = model.toMangaModel().toInfoModel()
                        if (model.numChapters >= newData.chapters.size) null
                        else Pair(newData, model)
                    } catch (e: Exception) {
                        println(e.localizedMessage)
                        null
                    }
                }
            }
            .map {
                Loged.f("Map2")
                it.forEach { triple ->
                    val manga = triple.second
                    manga.numChapters = triple.first.chapters.size
                    dao.updateMangaById(manga).subscribe()
                    FirebaseDb.updateManga(manga).subscribe()
                }
                update.mapDbModel(it)
            }
            .map {
                update.onEnd(it)
                Loged.f("Finished!")
            }
            .map {
                update.sendFinishedNotification()
                Result.success()
            }
            .onErrorReturn {
                update.sendFinishedNotification()
                Result.failure()
            }
    }

}

class UpdateNotification(private val context: Context) {

    fun mapDbModel(list: List<Pair<MangaInfoModel, MangaDbModel>>) = list.mapIndexed { index, pair ->
        sendRunningNotification(list.size, index, pair.second.title)
        pair.second.hashCode() to NotificationDslBuilder.builder(
            context,
            "mangaChannel",
            R.mipmap.ic_launcher
        ) {
            title = pair.second.title
            subText = pair.second.source.name
            getBitmapFromURL(pair.second.imageUrl)?.let {
                largeIconBitmap = it
                pictureStyle {
                    bigPicture = it
                    largeIcon = it
                    summaryText = context.getString(
                        R.string.hadAnUpdate,
                        pair.second.title,
                        pair.first.chapters.firstOrNull()?.name ?: ""
                    )
                }
            } ?: bigTextStyle {
                bigText = context.getString(
                    R.string.hadAnUpdate,
                    pair.second.title,
                    pair.first.chapters.firstOrNull()?.name.orEmpty()
                )
            }
            groupId = "mangaGroup"
            pendingIntent { context ->
                TaskStackBuilder.create(context)
                    .addParentStack(MainActivity::class.java)
                    .addNextIntent(Intent(context, MangaActivity::class.java).apply { putExtra("manga", pair.second.toMangaModel()) })
                    .getPendingIntent(pair.second.hashCode(), PendingIntent.FLAG_UPDATE_CURRENT)
            }
        }
    } to list.map { m -> m.second.toMangaModel() }

    fun onEnd(list: Pair<List<Pair<Int, Notification>>, List<MangaModel>>) {
        val n = context.notificationManager
        val currentNotificationSize = n.activeNotifications.filterNot { list.first.any { l -> l.first == it.id } }.size - 1
        list.first.forEach { pair -> n.notify(pair.first, pair.second) }
        if (list.first.isNotEmpty()) n.notify(
            42,
            NotificationDslBuilder.builder(context, "mangaChannel", R.mipmap.ic_launcher) {
                title = context.getText(R.string.app_name)
                val size = list.first.size + currentNotificationSize
                subText = context.resources.getQuantityString(R.plurals.updateAmount, size, size)
                groupSummary = true
                groupAlertBehavior = GroupBehavior.ALL
                groupId = "mangaGroup"
                if (context.canBubble) {
                    addBubble {
                        bubbleIntent(
                            PendingIntent.getActivity(
                                context, 0,
                                Intent(context, BubbleActivity::class.java).apply { putExtra("mangaList", list.second) },
                                0
                            )
                        )
                        desiredHeight = 600
                        icon = Icon.createWithResource(context, R.mipmap.ic_launcher)
                    }
                    messageStyle {
                        setMainPerson {
                            name = "MangaBot"
                            isBot = true
                        }
                        message {
                            message = context.resources.getQuantityString(
                                R.plurals.updateAmount,
                                list.first.size,
                                list.first.size
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

    fun sendRunningNotification(max: Int, progress: Int, contextText: CharSequence = "") {
        val notification = NotificationDslBuilder.builder(context, "updateCheckChannel", R.mipmap.ic_launcher) {
            onlyAlertOnce = true
            ongoing = true
            progress {
                this.max = max
                this.progress = progress
                indeterminate = progress == 0
            }
            message = contextText
            subText = context.getText(R.string.checkingUpdate)
        }
        context.notificationManager.notify(13, notification)
        Loged.f("Checking for $contextText")
    }

    fun sendFinishedNotification() {
        val notification = NotificationDslBuilder.builder(context, "updateCheckChannel", R.mipmap.ic_launcher) {
            onlyAlertOnce = true
            subText = context.getText(R.string.finishedChecking)
            timeoutAfter = 750L
        }
        context.notificationManager.notify(13, notification)
    }
}