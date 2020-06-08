package com.programmersbox.manga_sources.mangasources.utilities

import okhttp3.Request

fun Request.Builder.header(pair: Pair<String, String>) = header(pair.first, pair.second)
fun Request.Builder.header(vararg pair: Pair<String, String>) = apply { pair.forEach { header(it.first, it.second) } }