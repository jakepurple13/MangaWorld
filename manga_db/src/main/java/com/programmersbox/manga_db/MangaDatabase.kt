package com.programmersbox.manga_db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [MangaDbModel::class, MangaReadChapter::class], version = 1)
@TypeConverters(Converters::class)
abstract class MangaDatabase : RoomDatabase() {

    abstract fun mangaDao(): MangaDao

    companion object {

        @Volatile
        private var INSTANCE: MangaDatabase? = null

        fun getInstance(context: Context): MangaDatabase =
            INSTANCE ?: synchronized(this) { INSTANCE ?: buildDatabase(context).also { INSTANCE = it } }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(context.applicationContext, MangaDatabase::class.java, "manga.db").build()
    }
}