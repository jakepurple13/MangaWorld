package com.veinhorn.scrollgalleryview.loader

import android.content.Context
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.annotation.Nullable
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.veinhorn.scrollgalleryview.R
import com.veinhorn.scrollgalleryview.loader.MediaLoader.SuccessCallback


class GlideImageLoader : MediaLoader {
    private var url: String
    private var width = 0
    private var height = 0
    private var requestOptions: RequestOptions

    constructor(url: String) {
        this.url = url
        requestOptions = RequestOptions()
            .placeholder(R.drawable.placeholder_image)
    }

    constructor(url: String, width: Int, height: Int) {
        this.url = url
        this.width = width
        this.height = height
        requestOptions = RequestOptions()
            .placeholder(R.drawable.placeholder_image)
            .override(width, height)
    }

    override fun isImage(): Boolean {
        return true
    }

    override fun loadMedia(context: Context, imageView: ImageView, callback: SuccessCallback) {
        val requestOptions = RequestOptions()
        Glide.with(context).applyDefaultRequestOptions(requestOptions)
            .load(url).listener(object : RequestListener<Drawable?> {
                override fun onLoadFailed(
                    @Nullable e: GlideException?,
                    model: Any?,
                    target: Target<Drawable?>?,
                    isFirstResource: Boolean
                ): Boolean {
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable?>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    callback.onSuccess()
                    return false
                }
            }).into(imageView)
    }

    override fun loadThumbnail(context: Context, thumbnailView: ImageView, callback: SuccessCallback) {
        Glide.with(context).applyDefaultRequestOptions(requestOptions).load(url)
            .listener(object : RequestListener<Drawable?> {
                override fun onLoadFailed(
                    @Nullable e: GlideException?,
                    model: Any?,
                    target: Target<Drawable?>?,
                    isFirstResource: Boolean
                ): Boolean {
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable?>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    callback.onSuccess()
                    return false
                }
            }).into(thumbnailView)
    }
}