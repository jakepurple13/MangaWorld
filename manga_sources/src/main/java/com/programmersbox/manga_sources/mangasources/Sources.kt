package com.programmersbox.manga_sources.mangasources

enum class Sources {
    MANGA_EDEN, MANGANELO, MANGA_HERE, INKR, MANGA_4_LIFE, MANGA_DOG, TSUMINO;

    operator fun invoke() = source()
    fun source(): MangaSource = when (this) {
        MANGANELO -> Manganelo
        MANGA_EDEN -> MangaEden
        MANGA_HERE -> MangaHere
        INKR -> com.programmersbox.manga_sources.mangasources.INKR
        MANGA_4_LIFE -> MangaFourLife
        MANGA_DOG -> MangaDog
        TSUMINO -> Tsumino
    }
}
