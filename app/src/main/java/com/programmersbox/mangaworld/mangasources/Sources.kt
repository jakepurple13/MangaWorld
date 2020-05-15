package com.programmersbox.mangaworld.mangasources

enum class Sources {
    MANGA_EDEN, MANGANELO;

    fun source(): MangaSource = when (this) {
        MANGANELO -> Manganelo()
        MANGA_EDEN -> MangaEden()
    }
}