package com.programmersbox.manga_sources.mangasources.utilities

import com.programmersbox.manga_sources.mangasources.MangaContext
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.util.concurrent.TimeUnit

fun Request.Builder.header(pair: Pair<String, String>) = header(pair.first, pair.second)
fun Request.Builder.header(vararg pair: Pair<String, String>) = apply { pair.forEach { header(it.first, it.second) } }
fun Connection.headers(vararg pair: Pair<String, String>) = apply { headers(pair.toMap()) }

fun Response.asJsoup(html: String? = null): Document = Jsoup.parse(html ?: body!!.string(), request.url.toString())

internal fun cloudflare(url: String, vararg headers: Pair<String, String>) = MangaContext.helper.cloudflareClient.newCall(
    Request.Builder()
        .url(url)
        .header(*headers)
        .cacheControl(CacheControl.Builder().maxAge(10, TimeUnit.MINUTES).build())
        .build()
)

internal fun OkHttpClient.cloudflare(url: String, vararg headers: Pair<String, String>) = newCall(
    Request.Builder()
        .url(url)
        .header(*headers)
        .cacheControl(CacheControl.Builder().maxAge(10, TimeUnit.MINUTES).build())
        .build()
)