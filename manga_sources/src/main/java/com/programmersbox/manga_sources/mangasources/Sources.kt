package com.programmersbox.manga_sources.mangasources

enum class Sources {
    MANGA_EDEN, MANGANELO, MANGA_HERE, INKR;

    operator fun invoke() = source()
    fun source(): MangaSource = when (this) {
        MANGANELO -> Manganelo
        MANGA_EDEN -> MangaEden
        MANGA_HERE -> MangaHere
        INKR -> com.programmersbox.manga_sources.mangasources.INKR
    }
}
