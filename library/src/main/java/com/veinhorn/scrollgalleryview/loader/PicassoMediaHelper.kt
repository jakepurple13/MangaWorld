package com.veinhorn.scrollgalleryview.loader

import com.veinhorn.scrollgalleryview.MediaInfo
import com.veinhorn.scrollgalleryview.builder.BasicMediaHelper
import java.util.*


class PicassoMediaHelper : BasicMediaHelper() {
    override fun image(url: String): MediaInfo {
        return mediaInfo(url)
    }

    override fun images(urls: List<String>): List<MediaInfo> {
        val medias: MutableList<MediaInfo> = ArrayList()
        for (url in urls) {
            medias.add(mediaInfo(url))
        }
        return medias
    }

    override fun images(vararg urls: String): List<MediaInfo> {
        return images(Arrays.asList(*urls))
    }

    private fun mediaInfo(url: String): MediaInfo {
        return MediaInfo.mediaLoader(PicassoImageLoader(url))
    }
}