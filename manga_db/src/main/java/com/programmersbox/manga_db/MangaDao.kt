package com.programmersbox.manga_db

import androidx.room.*
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single

/**
 * Data Access Object for the users table.
 */
@Dao
interface MangaDao {

    @Query("SELECT * FROM FavoriteManga WHERE mangaUrl = :url")
    fun getMangaById(url: String): Single<MangaDbModel>

    @Update
    fun updateMangaById(model: MangaDbModel): Completable

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertManga(manga: MangaDbModel): Completable

    @Delete
    fun deleteManga(manga: MangaDbModel): Completable

    @Query("SELECT * FROM FavoriteManga")
    fun getAllManga(): Flowable<List<MangaDbModel>>

    @Query("SELECT * FROM FavoriteManga")
    fun getAllMangaSync(): List<MangaDbModel>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertChapter(chapter: MangaReadChapter): Completable

    @Delete
    fun deleteChapter(chapter: MangaReadChapter): Completable

    @Query("SELECT * FROM ChaptersRead WHERE mangaUrl = :url")
    fun getReadChaptersById(url: String): Flowable<List<MangaReadChapter>>

    @Query("SELECT * FROM ChaptersRead WHERE mangaUrl = :url")
    fun getReadChaptersByIdNonFlow(url: String): List<MangaReadChapter>

    @Query("SELECT * FROM ChaptersRead")
    fun getAllChapters(): List<MangaReadChapter>

}