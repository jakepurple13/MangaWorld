package com.programmersbox.mangaworld

import android.app.IntentService
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.TaskStackBuilder
import com.programmersbox.gsonutils.putExtra
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
        sendRunningNotification(100, 0, getText(R.string.startingUpdateCheck))
        val dao = MangaDatabase.getInstance(this@UpdateCheckService).mangaDao()
        GlobalScope.launch {
            dao.getAllMangaSync()
                .map { model -> Triple(model.numChapters, model.toMangaModel().toInfoModel(), model) }
                .filter { it.first < it.second.chapters.size }
                .also {
                    it.forEach { triple ->
                        val manga = triple.third
                        manga.numChapters = triple.second.chapters.size
                        dao.updateMangaById(manga)
                    }
                }
                .let {
                    it.mapIndexed { index, pair ->
                        sendRunningNotification(it.size, index, pair.second.title)
                        pair.second.hashCode() to NotificationDslBuilder.builder(this@UpdateCheckService, "mangaChannel", R.mipmap.ic_launcher) {
                            title = pair.second.title
                            subText = pair.third.source.name
                            bigTextStyle {
                                bigText = getString(R.string.hadAnUpdate, pair.second.title, pair.second.chapters.firstOrNull()?.name.orEmpty())
                            }
                            pendingIntent { context ->
                                TaskStackBuilder.create(context)
                                    .addParentStack(MainActivity::class.java)
                                    .addNextIntent(
                                        Intent(context, MangaActivity::class.java)
                                            .apply { putExtra("manga", pair.third.toMangaModel()) }
                                    )
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
                            title = getString(R.string.updateNumber, it.size)
                            groupSummary = true
                        }
                    )
                    sendFinishedNotification()
                }
        }
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

}