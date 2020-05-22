package com.veinhorn.scrollgalleryview.loader

import android.content.Context
import android.widget.ImageView
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import com.veinhorn.scrollgalleryview.R
import com.veinhorn.scrollgalleryview.loader.MediaLoader.SuccessCallback

/**
 * Created by veinhorn on 2/4/18.
 */
class PicassoImageLoader : MediaLoader {
    private var url: String
    private var thumbnailWidth: Int? = null
    private var thumbnailHeight: Int? = null

    constructor(url: String) {
        this.url = url
    }

    constructor(url: String, thumbnailWidth: Int?, thumbnailHeight: Int?) {
        this.url = url
        this.thumbnailWidth = thumbnailWidth
        this.thumbnailHeight = thumbnailHeight
    }

    override fun isImage(): Boolean {
        return true
    }

    override fun loadMedia(context: Context, imageView: ImageView, callback: SuccessCallback) {
        Picasso.get()
            .load(url)
            .placeholder(R.drawable.placeholder_image)
            .into(imageView, ImageCallback(callback))
    }

    override fun loadThumbnail(
        context: Context,
        thumbnailView: ImageView,
        callback: SuccessCallback
    ) {
        Picasso.get()
            .load(url)
            .resize(
                thumbnailWidth ?: 100,
                thumbnailHeight ?: 100
            )
            .placeholder(R.drawable.placeholder_image)
            .centerInside()
            .into(thumbnailView, ImageCallback(callback))
    }

    private class ImageCallback(private val callback: SuccessCallback) : Callback {
        override fun onSuccess() {
            callback.onSuccess()
        }

        override fun onError(e: Exception?) {}

    }
}