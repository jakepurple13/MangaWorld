package com.programmersbox.manga_sources.mangasources

import com.programmersbox.manga_sources.mangasources.manga.*

enum class Sources(val isAdult: Boolean = false, val filterOutOfUpdate: Boolean = false) : MangaSource {
    MANGA_EDEN(filterOutOfUpdate = true),
    MANGANELO,
    MANGA_HERE,
    INKR,
    MANGA_4_LIFE,
    NINE_ANIME,
    MANGAKAKALOT,

    //MANGA_DOG,
    TSUMINO(isAdult = true);

    override val hasMorePages: Boolean get() = source().hasMorePages
    override fun getManga(pageNumber: Int): List<MangaModel> = source().getManga(pageNumber)
    override fun getPageInfo(chapterModel: ChapterModel): PageModel = source().getPageInfo(chapterModel)
    override fun toInfoModel(model: MangaModel): MangaInfoModel = source().toInfoModel(model)
    override fun searchManga(searchText: CharSequence, pageNumber: Int, mangaList: List<MangaModel>): List<MangaModel> =
        source().searchManga(searchText, pageNumber, mangaList)

    operator fun invoke() = source()
    fun source(): MangaSource = when (this) {
        MANGANELO -> Manganelo
        MANGA_EDEN -> MangaEden
        MANGA_HERE -> MangaHere
        INKR -> com.programmersbox.manga_sources.mangasources.manga.INKR
        MANGA_4_LIFE -> MangaFourLife
        NINE_ANIME -> NineAnime
        MANGAKAKALOT -> Mangakakalot
        //MANGA_DOG -> MangaDog
        TSUMINO -> Tsumino
    }
}
