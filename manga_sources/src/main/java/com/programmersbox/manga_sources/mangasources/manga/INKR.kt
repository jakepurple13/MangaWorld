package com.programmersbox.manga_sources.mangasources.manga

import androidx.annotation.WorkerThread
import com.programmersbox.gsonutils.fromJson
import com.programmersbox.gsonutils.getJsonApi
import com.programmersbox.gsonutils.toJson
import com.programmersbox.manga_sources.mangasources.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.experimental.and
import kotlin.experimental.xor

object INKR : MangaSource {

    private const val apiUrl = "https://api.mangarockhd.com/query/android500"

    override val websiteUrl: String = "https://www.inkr.com"

    //TODO: Work in progress
    private fun searchMangaTest(searchText: CharSequence, pageNumber: Int, mangaList: List<MangaModel>): List<MangaModel> = try {
        if (searchText.isBlank()) throw Exception("No search necessary")

        val jsonType = "application/jsonType; charset=utf-8".toMediaTypeOrNull()

        val client = OkHttpClient()

        val request = Request.Builder()
            .url("$apiUrl/mrs_search")
            .post(mapOf("type" to "series", "keywords" to searchText).toJson().toRequestBody(jsonType))
            .cacheControl(CacheControl.Builder().maxAge(10, TimeUnit.MINUTES).build())
            .build()

        val response = client.newCall(request).execute()

        val idArray = JSONObject(response.body!!.string()).getJSONArray("data")

        val request2 = Request.Builder()
            .url("https://api.mangarockhd.com/meta")
            .post(RequestBody.create(jsonType, idArray.toString()))
            .cacheControl(CacheControl.Builder().maxAge(10, TimeUnit.MINUTES).build())
            .build()

        val response2 = client.newCall(request2).execute().body?.string() ?: throw Exception("Something went wrong")

        val list = mutableListOf<MangaModel>()

        val json = JSONObject(response2).getJSONObject("data")

        for (i in 0 until idArray.length()) {
            val id = idArray.get(i).toString()
            list.add(
                json.getJSONObject(id).let {
                    MangaModel(
                        title = it.getString("name"),
                        description = "Total Chapters: ${it.getInt("total_chapters")}",
                        mangaUrl = "$apiUrl/info?oid=${it.getString("oid")}&Country=",
                        imageUrl = it.getString("thumbnail"),
                        source = Sources.INKR
                    )
                }
            )
        }

        list
    } catch (e: Exception) {
        super.searchManga(searchText, pageNumber, mangaList)
    }

    override fun getManga(pageNumber: Int): List<MangaModel> = getJsonApi<InkrJson>("$apiUrl/mrs_latest")?.data?.map {
        MangaModel(
            title = it.name.toString(),
            description = it.updated_at.toString(),
            mangaUrl = "$apiUrl/info?oid=${it.oid}&Country=",
            imageUrl = it.thumbnail.toString(),
            source = Sources.INKR
        )
    }.orEmpty()

    override fun toInfoModel(model: MangaModel): MangaInfoModel = getJsonApi<InkrInfoBase>(model.mangaUrl)?.data.let {
        MangaInfoModel(
            title = model.title,
            description = it?.description ?: model.description,
            mangaUrl = model.mangaUrl,
            imageUrl = it?.thumbnail ?: model.imageUrl,
            chapters = it?.chapters?.map {
                ChapterModel(
                    name = it.name.toString(),
                    url = "$apiUrl/pagesv3?oid=${it.oid}",
                    uploaded = "Last updated: ${SimpleDateFormat(
                        "MM/dd/yyyy hh:mm a",
                        Locale.getDefault()
                    ).format(1000 * (it.updatedAt?.toDouble() ?: 0.0))}",
                    sources = Sources.INKR
                ).apply { uploadedTime = it.updatedAt?.let { t -> 1000 * t.toLong() } }
            }.orEmpty(),
            genres = it?.rich_categories?.map { it.name.toString() }.orEmpty(),
            alternativeNames = it?.alias.orEmpty()
        )
    }

    override fun getMangaModelByUrl(url: String): MangaModel = getJsonApi<InkrInfoBase>(url)?.data.let {
        MangaModel(
            title = it?.name.orEmpty(),
            description = it?.description.orEmpty(),
            mangaUrl = url,
            imageUrl = it?.thumbnail.orEmpty(),
            source = Sources.INKR
        )
    }

    private val client: OkHttpClient = OkHttpClient.Builder().addInterceptor(fun(chain): Response {
        val url = chain.request().url.toString()
        val response = chain.proceed(chain.request())
        if (!url.endsWith(".mri")) return response

        val decoded: ByteArray = decodeMri(response)
        val mediaType = "image/webp".toMediaTypeOrNull()
        val rb = ResponseBody.create(mediaType, decoded)
        return response.newBuilder().body(rb).build()
    }).build()

    //TODO: Still working on this
    override fun getPageInfo(chapterModel: ChapterModel): PageModel =
        PageModel(
            getJsonApis<InkrPages>(
                chapterModel.url
            )?.data?.map { it.url.toString() }.orEmpty()
        )

    @WorkerThread
    private fun getApis(url: String): String? {
        val request = Request.Builder()
            .url(url)
            .get()
            .build()
        val response = client.newCall(request).execute()
        return if (response.code == 200) response.body!!.string() else null
    }

    @WorkerThread
    private inline fun <reified T> getJsonApis(url: String) = getApis(url)
        .fromJson<T>()

    override val hasMorePages: Boolean = false

    private data class InkrJson(val code: Number?, val data: List<Data>?)

    private data class Data(
        val oid: String?,
        val name: String?,
        val genres: List<String>?,
        val rank: Number?,
        val updated_chapters: Number?,
        val new_chapters: List<NewChapters>?,
        val completed: Boolean?,
        val thumbnail: String?,
        val updated_at: String?
    )

    private data class NewChapters(val oid: String?, val name: String?, val updatedAt: String?)

    private data class Authors(val oid: String?, val name: String?, val thumbnail: String?, val role: String?)

    private data class InkrInfoBase(val code: Number?, val data: InkrData?)

    private data class InkrData(
        val mid: Number?,
        val oid: String?,
        val name: String?,
        val author: String?,
        val rank: Number?,
        val msid: Number?,
        val completed: Boolean?,
        val last_update: Number?,
        val removed: Boolean?,
        val direction: Number?,
        val total_chapters: Number?,
        val description: String?,
        val categories: List<Number>?,
        val chapters: List<Chapters>?,
        val thumbnail: String?,
        val cover: String?,
        val artworks: List<Any>?,
        val alias: List<String>?,
        val characters: List<String>?,
        val authors: List<Authors>?,
        val rich_categories: List<RichCategories>?,
        val extra: Extra?,
        val mrs_series: Any?
    )

    private class Extra

    private data class RichCategories(val oid: String?, val name: String?)

    private data class Chapters(val cid: Number?, val oid: String?, val order: Number?, val name: String?, val updatedAt: Number?)

    data class InkrPages(val code: Number?, val data: List<InkrPage>?)

    data class InkrPage(val role: String?, val url: String?, val width: Number?, val height: Number?)

    private fun decodeMri(response: Response): ByteArray {
        val data = response.body!!.bytes()

        // Decode file if it starts with "E" (space when XOR-ed later)
        if (data[0] != 69.toByte()) return data

        // Reconstruct WEBP header
        // Doc: https://developers.google.com/speed/webp/docs/riff_container#webp_file_header
        val buffer = ByteArray(data.size + 15)
        val size = data.size + 7
        buffer[0] = 82  // R
        buffer[1] = 73  // I
        buffer[2] = 70  // F
        buffer[3] = 70  // F
        buffer[4] = (255.toByte() and size.toByte())
        buffer[5] = (size ushr 8).toByte() and 255.toByte()
        buffer[6] = (size ushr 16).toByte() and 255.toByte()
        buffer[7] = (size ushr 24).toByte() and 255.toByte()
        buffer[8] = 87  // W
        buffer[9] = 69  // E
        buffer[10] = 66 // B
        buffer[11] = 80 // P
        buffer[12] = 86 // V
        buffer[13] = 80 // P
        buffer[14] = 56 // 8

        // Decrypt file content using XOR cipher with 101 as the key
        val cipherKey = 101.toByte()
        for (r in data.indices) {
            buffer[r + 15] = cipherKey xor data[r]
        }

        return buffer
    }

}