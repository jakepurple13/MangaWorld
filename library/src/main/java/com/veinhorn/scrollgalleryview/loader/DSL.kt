package com.veinhorn.scrollgalleryview.loader

import com.veinhorn.scrollgalleryview.MediaInfo

object DSL {
    private val mediaHelper: PicassoMediaHelper = PicassoMediaHelper()
    fun image(url: String): MediaInfo {
        return mediaHelper.image(url)
    }

    fun images(urls: List<String>): List<MediaInfo> {
        return mediaHelper.images(urls)
    }

    fun images(vararg urls: String): List<MediaInfo> {
        return mediaHelper.images(*urls)
    }

    fun video(url: String?, placeholderViewId: Int): MediaInfo {
        return mediaHelper.video(url, placeholderViewId)
    }
}