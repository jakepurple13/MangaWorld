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

    /**
     * Get a user by id.

     * @return the user from the table with a specific id.
     */
    @Query("SELECT * FROM FavoriteManga WHERE mangaUrl = :url")
    fun getMangaById(url: String): Single<MangaDbModel>

    /**
     * Insert a user in the database. If the user already exists, replace it.

     * @param manga the user to be inserted.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertManga(manga: MangaDbModel): Completable

    @Delete
    fun deleteManga(manga: MangaDbModel): Completable

    @Query("SELECT * FROM FavoriteManga")
    fun getAllManga(): Flowable<List<MangaDbModel>>

}