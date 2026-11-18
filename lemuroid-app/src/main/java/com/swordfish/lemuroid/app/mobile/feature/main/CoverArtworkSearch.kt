package com.swordfish.lemuroid.app.mobile.feature.main

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

/** Bounded, explicit searches of the public Wikipedia PageImages API. No Google scraping. */
internal object CoverArtworkSearch {
    data class Result(
        val title: String,
        val pageId: Long,
        val imageUrl: String,
    )

    private const val MAX_BYTES = 4 * 1024 * 1024
    private const val AGENT = "CARTDECK-CoverSearch/1.0 (https://github.com/safetyblade/ROMMachine)"
    private val client =
        OkHttpClient.Builder()
            .followRedirects(false)
            .followSslRedirects(false)
            .callTimeout(20, TimeUnit.SECONDS)
            .build()

    fun find(query: String): List<Result> {
        require(query.isNotBlank() && query.length <= 180)
        val url =
            "https://en.wikipedia.org/w/api.php".toHttpUrl().newBuilder()
                .addQueryParameter("action", "query")
                .addQueryParameter("format", "json")
                .addQueryParameter("formatversion", "2")
                .addQueryParameter("generator", "search")
                .addQueryParameter("gsrsearch", query)
                .addQueryParameter("gsrnamespace", "0")
                .addQueryParameter("gsrlimit", "10")
                .addQueryParameter("prop", "pageimages")
                .addQueryParameter("piprop", "thumbnail")
                .addQueryParameter("pithumbsize", "640")
                .addQueryParameter("pilicense", "any")
                .addQueryParameter("pilimit", "10")
                .build()
        val json = JSONObject(String(read(url.toString()), Charsets.UTF_8))
        require(!json.has("error")) { "Image search unavailable" }
        val pages = json.optJSONObject("query")?.optJSONArray("pages") ?: return emptyList()
        return (0 until pages.length()).mapNotNull { index ->
            val page = pages.getJSONObject(index)
            val image = page.optJSONObject("thumbnail")?.optString("source").orEmpty()
            val id = page.optLong("pageid")
            if (id <= 0 || !isImageUrl(image)) null else Result(page.getString("title"), id, image)
        }
    }

    fun isImageUrl(value: String): Boolean {
        val url = value.toHttpUrlOrNull() ?: return false
        return url.isHttps && url.host == "upload.wikimedia.org" &&
            url.port == 443 && url.username.isEmpty() && url.password.isEmpty()
    }

    fun load(result: Result): Bitmap {
        require(isImageUrl(result.imageUrl))
        val bytes = read(result.imageUrl)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        require(bounds.outWidth in 1..2048 && bounds.outHeight in 1..2048) { "Unsupported image dimensions" }
        return requireNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size)) { "Invalid image" }
    }

    private fun read(address: String): ByteArray {
        val request = Request.Builder().url(address).header("User-Agent", AGENT).build()
        client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "Artwork service unavailable" }
            val body = checkNotNull(response.body)
            require(body.contentLength() <= MAX_BYTES)
            return body.byteStream().use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                var count = input.read(buffer)
                while (count != -1) {
                    require(output.size() + count <= MAX_BYTES) { "Artwork too large" }
                    output.write(buffer, 0, count)
                    count = input.read(buffer)
                }
                output.toByteArray()
            }
        }
    }
}
