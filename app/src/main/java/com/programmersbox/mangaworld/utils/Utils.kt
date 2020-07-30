package com.programmersbox.mangaworld.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.programmersbox.gsonutils.fromJson
import com.programmersbox.gsonutils.getObject
import com.programmersbox.gsonutils.putObject
import com.programmersbox.gsonutils.sharedPrefNotNullObjectDelegate
import com.programmersbox.helpfulutils.*
import com.programmersbox.loggingutils.Loged
import com.programmersbox.loggingutils.f
import com.programmersbox.manga_db.MangaDbModel
import com.programmersbox.manga_sources.mangasources.ChapterModel
import com.programmersbox.manga_sources.mangasources.MangaInfoModel
import com.programmersbox.manga_sources.mangasources.MangaModel
import com.programmersbox.manga_sources.mangasources.Sources
import com.programmersbox.mangaworld.R
import com.programmersbox.rxutils.invoke
import com.programmersbox.thirdpartyutils.openInCustomChromeBrowser
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import zlc.season.rxdownload4.delete
import zlc.season.rxdownload4.download
import zlc.season.rxdownload4.file
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import java.nio.channels.FileChannel

var Context.usePalette: Boolean by sharedPrefNotNullDelegate(true)
var Context.currentSource: Sources by sharedPrefNotNullObjectDelegate(defaultValue = Sources.values().filterNot(Sources::isAdult).random())
var Context.useCache: Boolean by sharedPrefNotNullDelegate(true)
var Context.cacheSize: Int by sharedPrefNotNullDelegate(5)
var Context.stayOnAdult: Boolean by sharedPrefNotNullDelegate(false)
var Context.groupManga: Boolean by sharedPrefNotNullDelegate(true)
var Context.chapterHistory: List<ChapterHistory> by sharedPrefNotNullObjectDelegate(emptyList())
var Context.chapterHistorySize: Int by sharedPrefNotNullDelegate(50)
var Context.batteryAlertPercent: Float by sharedPrefNotNullDelegate(20f)
var Context.canBubble: Boolean by sharedPrefNotNullDelegate(true)
var Context.useAgo: Boolean by sharedPrefNotNullDelegate(false)
var Context.useUpdate: Boolean by sharedPrefNotNullObjectDelegate(true)

fun Context.changeChapterHistorySize(size: Int) {
    chapterHistorySize = size
    val history = chapterHistory.toMutableList()
    if (history.size > size) chapterHistory = history.take(size)
}

data class ChapterHistory(val mangaUrl: String, val imageUrl: String, val title: String, val chapterModel: ChapterModel) : ViewModel() {
    fun toChapterString() = "${chapterModel.name}\n${chapterModel.uploaded}"
}

fun <T> MutableList<T>.addMax(item: T, maxSize: Int) {
    add(0, item)
    if (size > maxSize) removeAt(lastIndex)
}

fun Context.addToHistory(ch: ChapterHistory) {
    val history = chapterHistory.toMutableList()
    if (!history.any { it.chapterModel.url == ch.chapterModel.url })
        history.addMax(ch, chapterHistorySize)
    chapterHistory = history
}

object MangaInfoCache {
    private lateinit var context: Context
    fun init(context: Context) = run { this.context = context }

    fun getInfo() =
        if (context.useCache) context.defaultSharedPref.getObject<List<MangaInfoModel>>("mangaInfoCache", defaultValue = emptyList()) else null

    fun newInfo(mangaInfoModel: MangaInfoModel) {
        if (context.useCache) {
            val list = getInfo()!!.toMutableList()
            if (mangaInfoModel !in list) list.add(0, mangaInfoModel)
            if (list.size > context.cacheSize) list.removeAt(list.lastIndex)
            context.defaultSharedPref.edit().putObject("mangaInfoCache", list.distinctBy(MangaInfoModel::mangaUrl)).apply()
        }
    }
}

fun MangaDbModel.toMangaModel() = MangaModel(title, description, mangaUrl, imageUrl, source)
fun MangaModel.toMangaDbModel(numChapters: Int? = null) = MangaDbModel(title, description, mangaUrl, imageUrl, source)
    .apply { if (numChapters != null) this.numChapters = numChapters }

enum class MangaListView {
    LINEAR, GRID;

    operator fun not() = when (this) {
        LINEAR -> GRID
        GRID -> LINEAR
    }
}

@Throws(IOException::class)
fun moveFile(file: File, dir: File) {
    val newFile = File(dir, file.name)
    var outputChannel: FileChannel? = null
    var inputChannel: FileChannel? = null
    try {
        outputChannel = FileOutputStream(newFile).channel
        inputChannel = FileInputStream(file).channel
        inputChannel.transferTo(0, inputChannel.size(), outputChannel)
        inputChannel.close()
        file.delete()
    } finally {
        inputChannel?.close()
        outputChannel?.close()
    }
}

class DownloadedManga {

    data class ChapterDownload(val title: String, val path: String, val pages: List<String>)
    data class MangaDownload(val title: String, val path: String, val pages: List<ChapterDownload>)

    private val fileLocation by lazy {
        File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + "/MangaWorld/")
    }

    fun permissionCheck(activity: ComponentActivity) {
        activity.requestPermissions(Manifest.permission.READ_EXTERNAL_STORAGE) {
            if (!it.isGranted) Toast.makeText(activity, "Need Permission", Toast.LENGTH_SHORT).show()
        }
    }

    fun loadDownloaded() = fileLocation.listFiles()?.mapNotNull {
        if (it.isDirectory) {
            MangaDownload(
                it.name,
                it.path,
                it.listFiles()?.map { c -> ChapterDownload(c.name, c.path, c.list()?.toList().orEmpty()) }.orEmpty()
            )
        } else null
    }
        .orEmpty()
        .also { Loged.f(it) }

    fun rxLoad() = Single.create<List<MangaDownload>> { emitter ->
        try {
            val list = fileLocation.listFiles()?.map {
                MangaDownload(
                    it.name,
                    it.path,
                    it.listFiles()?.map { c -> ChapterDownload(c.name, c.path, c.list()?.toList() ?: emptyList()) } ?: emptyList()
                )
            }
                .orEmpty()
            emitter(list)
        } catch (e: Exception) {
            emitter(e)
        }
    }
}

data class AppInfo(val version: String, val url: String, val releaseNotes: List<String> = emptyList())

class AppUpdateChecker(private val activity: ComponentActivity) {

    private val context: Context = activity

    private val updateUrl = "https://raw.githubusercontent.com/jakepurple13/MangaWorld/master/app/src/main/res/raw/update_changelog.json"

    private val disposable = CompositeDisposable()

    @Suppress("RedundantSuspendModifier", "BlockingMethodInNonBlockingContext")
    suspend fun checkForUpdate() {
        try {
            val url = URL(updateUrl).readText()
            val info = url.fromJson<AppInfo>()!!
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val version = pInfo.versionName
            Loged.f("Current Version: $version | Server Version: ${info.version}")
            if (version.toDouble() < info.version.toDouble()) installUpdate(info) else deleteApk(info)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun deleteApk(info: AppInfo) {
        val file =
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + "/" + "mangaworld${info.version}.apk")
        if (file.exists()) file.delete()
    }

    private fun installUpdate(info: AppInfo) {
        activity.requestPermissions(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) {
            if (it.isGranted) {
                runOnUIThread {
                    MaterialAlertDialogBuilder(context)
                        .setTitle(context.getString(R.string.updateTitle, info.version))
                        .setItems(info.releaseNotes.toTypedArray(), null)
                        .setPositiveButton(R.string.update) { d, _ ->
                            download2(info)
                            d.dismiss()
                        }
                        .setNeutralButton(R.string.gotoBrowser) { d, _ ->
                            context.openInCustomChromeBrowser("https://github.com/jakepurple13/MangaWorld/releases/latest") {
                                setStartAnimations(context, R.anim.slide_in_right, R.anim.slide_out_left)
                            }
                            d.dismiss()
                        }
                        .setNegativeButton(R.string.notNow) { d, _ -> d.dismiss() }
                        .show()
                }
            }
        }

    }

    private fun download2(info: AppInfo) {
        val direct = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!.absolutePath + "/")

        if (!direct.exists()) {
            direct.mkdir()
        }

        val request = DownloadDslManager(context) {
            downloadUri = Uri.parse(info.url)
            allowOverRoaming = true
            networkType = DownloadDslManager.NetworkType.WIFI_MOBILE
            title = context.getString(R.string.mangaWorldUpdate, info.version)
            mimeType = "application/vnd.android.package-archive"
            visibility = DownloadDslManager.NotificationVisibility.COMPLETED
            destinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS, File.separator + "mangaworld${info.version}.apk"
            )
        }

        context.downloadManager.enqueue(request)
    }

    private fun download(info: AppInfo) {
        val n = context.notificationManager

        val finished = NotificationDslBuilder.builder(context, "appUpdate", R.drawable.manga_world_round_logo) {
            subText = "Downloaded Update ${info.version}"
            timeoutAfter = 750L
            onlyAlertOnce = true
        }

        info.url.download()
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .doOnSubscribe {
                n.notify(2, NotificationDslBuilder.builder(context, "appUpdate", R.drawable.manga_world_round_logo) {
                    subText = "Downloading Update ${info.version}"
                    ongoing = true
                    onlyAlertOnce = true
                    progress {
                        max = 100
                        progress = 0
                        indeterminate = true
                    }
                })
            }
            .subscribeBy(
                onNext = { progress ->
                    //download progress
                    n.notify(
                        2,
                        NotificationDslBuilder.builder(context, "appUpdate", R.drawable.manga_world_round_logo) {
                            message = "Downloading Update ${info.version}"
                            subText = "Downloading...${progress.percentStr()}"
                            ongoing = true
                            onlyAlertOnce = true
                            progress {
                                max = 100
                                this.progress = progress.percent().toInt()
                            }
                        }
                    )
                },
                onComplete = {
                    //download complete
                    n.notify(2, finished)
                    if (info.url.file().exists()) install(info)
                },
                onError = {
                    n.notify(2, finished)
                    //download failed
                    info.url.delete()
                }
            )
            .addTo(disposable)
    }

    private fun install(info: AppInfo) {
        disposable.dispose()
        val strApkToInstall = info.url.file()
        // val path1 = File(File(Environment.getExternalStorageDirectory(), "Download"), strApkToInstall)

        val apkUri = FileProvider.getUriForFile(context, context.packageName + ".utils.GenericFileProvider", strApkToInstall)
        val intent = Intent(Intent.ACTION_INSTALL_PACKAGE)
        intent.data = apkUri
        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        //intent.setDataAndType(apkUri, "application/vnd.android.package-archive")
        context.startActivity(intent)
    }

}

class GenericFileProvider : FileProvider()