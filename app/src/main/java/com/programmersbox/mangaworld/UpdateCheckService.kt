package com.programmersbox.mangaworld

import android.app.IntentService
import android.content.Intent
import com.programmersbox.helpfulutils.NotificationDslBuilder
import com.programmersbox.helpfulutils.notificationManager
import com.programmersbox.loggingutils.Loged
import com.programmersbox.loggingutils.f
import com.programmersbox.manga_db.MangaDatabase
import com.programmersbox.mangaworld.utils.toMangaModel
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class UpdateCheckService : IntentService("UpdateCheckIntentService") {

    private val disposable = CompositeDisposable()

    override fun onUnbind(intent: Intent?): Boolean {
        notificationManager.cancel(42)
        disposable.dispose()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        notificationManager.cancel(42)
        disposable.dispose()
        super.onDestroy()
    }

    override fun onHandleIntent(intent: Intent?) {
        sendRunningNotification(100, 0, "Starting Manga Update Checks")
        GlobalScope.launch {
            MangaDatabase.getInstance(this@UpdateCheckService).mangaDao().getAllMangaSync()
                .map { model -> Triple(model.numChapters, model.toMangaModel().toInfoModel(), model.source) }
                .filter { it.first < it.second.chapters.size }
                .let {
                    it.mapIndexed { index, pair ->
                        sendRunningNotification(it.size, index, pair.second.title)
                        pair.second.hashCode() to NotificationDslBuilder.builder(this@UpdateCheckService, "mangaChannel", R.mipmap.ic_launcher) {
                            title = pair.second.title
                            subText = pair.third.name
                            bigTextStyle {
                                bigText = "${pair.second.title} had an update. ${pair.second.chapters.firstOrNull()?.name}"
                            }
                        }
                    }
                }
                .let {
                    val n = notificationManager
                    it.forEach { n.notify(it.first, it.second) }
                    if (it.isNotEmpty()) n.notify(
                        42,
                        NotificationDslBuilder.builder(this@UpdateCheckService, "mangaChannel", R.mipmap.ic_launcher) {
                            title = "There are ${it.size} update(s)"
                            groupSummary = true
                        }
                    )
                    sendFinishedNotification()
                }
        }
    }

    private fun sendRunningNotification(max: Int, progress: Int, contextText: String = "") {
        val notification = NotificationDslBuilder.builder(this, "updateCheckChannel", R.mipmap.ic_launcher) {
            onlyAlertOnce = true
            ongoing = true
            progress {
                this.max = max
                this.progress = progress
                indeterminate = progress == 0
            }
            message = contextText
            subText = "Checking for Manga Updates"
        }
        notificationManager.notify(13, notification)
        Loged.f("Checking for $contextText")
    }

    private fun sendFinishedNotification() {
        val notification = NotificationDslBuilder.builder(this, "updateCheckChannel", R.mipmap.ic_launcher) {
            onlyAlertOnce = true
            subText = "Finished Checking for Manga Updates"
            timeoutAfter = 750L
        }
        notificationManager.notify(13, notification)
    }

    /*
    private fun sendRunningNotification(context: Context, smallIconId: Int, channel_id: String, notification_id: Int, prog: Int = 0, max: Int = 100, showCheckName: String = "") {
        // The id of the channel.
        val mBuilder = NotificationCompat.Builder(context, "updateCheckRun")
                .setSmallIcon(smallIconId)
                .setContentTitle("Checking for Show Updates${if (prog != 0) ": $prog/$max" else ""}")
                .setChannelId(channel_id)
                .setProgress(max, prog, prog == 0)
                .setOngoing(true)
                .setVibrate(longArrayOf(0L))
                .setContentText(showCheckName)
                .setSubText("Checking for Show Updates")
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)

        val mNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // mNotificationId is a unique integer your app uses to identify the
        // notification. For example, to cancel the notification, you can pass its ID
        // number to NotificationManager.cancel().
        mNotificationManager.notify(notification_id, mBuilder.build())
    }

    private fun sendFinishedCheckingNotification(context: Context, smallIconId: Int, channel_id: String, notification_id: Int, title: String = "Finished Checking", subText: String = "Finished Checking") {
        // The id of the channel.
        val mBuilder = NotificationCompat.Builder(context, "updateCheckRun")
                .setSmallIcon(smallIconId)
                .setContentTitle(title)
                .setSubText(subText)
                .setChannelId(channel_id)
                .setVibrate(longArrayOf(0L))
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setTimeoutAfter(750)

        val mNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // mNotificationId is a unique integer your app uses to identify the
        // notification. For example, to cancel the notification, you can pass its ID
        // number to NotificationManager.cancel().
        mNotificationManager.notify(notification_id, mBuilder.build())
    }
     */
}