package ru.aipova.locatr.network

import android.location.Location
import android.util.Log
import ru.aipova.locatr.model.GalleryItem

class FlickrRepository(val flickrApi: FlickrApi, val apiKey: String) {
    fun searchByLocation(location: Location): List<GalleryItem> {
        val response = flickrApi.search(lat = location.latitude.toString(), lon = location.longitude.toString(), apiKey = apiKey).execute()
        if (response.isSuccessful) {
            return response.body()?.photos?.items ?: listOf()
        } else {
            Log.e("FlickrRepository", "Response is not successful ${response.errorBody()}")
        }
        return listOf()
    }

}