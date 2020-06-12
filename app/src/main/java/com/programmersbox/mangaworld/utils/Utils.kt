package com.programmersbox.mangaworld.utils

import android.content.Context
import androidx.lifecycle.ViewModel
import com.programmersbox.gsonutils.getObject
import com.programmersbox.gsonutils.putObject
import com.programmersbox.gsonutils.sharedPrefNotNullObjectDelegate
import com.programmersbox.helpfulutils.defaultSharedPref
import com.programmersbox.helpfulutils.sharedPrefNotNullDelegate
import com.programmersbox.manga_db.MangaDbModel
import com.programmersbox.manga_sources.mangasources.ChapterModel
import com.programmersbox.manga_sources.mangasources.MangaInfoModel
import com.programmersbox.manga_sources.mangasources.MangaModel
import com.programmersbox.manga_sources.mangasources.Sources
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
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