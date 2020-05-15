package com.programmersbox.mangaworld.mangasources

enum class Sources {
    MANGA_EDEN, MANGANELO;

    fun getSource(): MangaSource = when (this) {
        MANGANELO -> Manganelo()
        MANGA_EDEN -> MangaEden()
    }
}