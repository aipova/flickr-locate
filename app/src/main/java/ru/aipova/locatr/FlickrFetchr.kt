package ru.aipova.locatr

import android.content.Context
import android.location.Location
import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import org.json.JSONException
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class FlickrFetchr(ctx: Context) {
    private val flickrEndpoint = buildEndpointUrl(ctx)

    private fun buildEndpointUrl(context: Context): Uri {
        return Uri.parse(API_PATH).buildUpon()
            .appendQueryParameter("api_key", context.getString(R.string.flickr_key))
            .appendQueryParameter("format", "json")
            .appendQueryParameter("nojsoncallback", "1")
            .appendQueryParameter("extras", "url_s,geo")
            .appendQueryParameter("per_page", "50")
            .build()
    }

    fun searchPhotos(location: Location): MutableList<GalleryItem> {
        val url = buildFlickrRequestUrl(location)
        return downloadGalleryItems(url)
    }

    private fun buildFlickrRequestUrl(location: Location): String {
        return flickrEndpoint.buildUpon()
            .appendQueryParameter("method", SEARCH)
            .appendQueryParameter("lat", location.latitude.toString())
            .appendQueryParameter("lon", location.longitude.toString())
            .build().toString()
    }

    private fun downloadGalleryItems(url: String): MutableList<GalleryItem> {
        val items = mutableListOf<GalleryItem>()
        try {
            val jsonStrong = getUtlString(url)
            Log.i(TAG, "Received JSON: $jsonStrong")
            val response = Gson().fromJson(jsonStrong, FlickrResponse::class.java)
            items.addAll(response.photos.items.filterNot { it.url.isNullOrEmpty() })

        } catch (ioe: IOException) {
            Log.e(TAG, "Failed to fetch items", ioe)
        } catch (je: JSONException) {
            Log.e(TAG, "Failed to parse JSON", je)
        }
        return items
    }

    private fun getUtlString(urlSpec: String): String {
        val url = URL(urlSpec)
        val connection = url.openConnection() as HttpURLConnection
        try {
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    fun getUtlBytes(urlSpec: String): ByteArray {
        val url = URL(urlSpec)
        val connection = url.openConnection() as HttpURLConnection
        try {
            return connection.inputStream.use { it.readBytes(1024) }
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        private val TAG = FlickrFetchr::class.java.name
        private const val API_PATH = "https://api.flickr.com/services/rest/"
        private const val GET_RECENT = "flickr.photos.getRecent"
        private const val SEARCH = "flickr.photos.search"
    }
}