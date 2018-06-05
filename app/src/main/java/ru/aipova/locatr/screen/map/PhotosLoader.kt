package ru.aipova.locatr.screen.map

import android.content.Context
import android.location.Location
import android.support.v4.content.AsyncTaskLoader
import android.util.Log
import retrofit2.Response
import ru.aipova.locatr.LocatrApp
import ru.aipova.locatr.model.FlickrResponse
import ru.aipova.locatr.model.GalleryMapItem
import ru.aipova.locatr.model.GalleryMapResult
import ru.aipova.locatr.network.ApiFactory
import java.io.IOException

class PhotosLoader(context: Context, private val location: Location) :
    AsyncTaskLoader<GalleryMapResult>(context) {

    private var galleryMapResult: GalleryMapResult? = null

    override fun onStartLoading() {
        super.onStartLoading()
        if (galleryMapResult != null) {
            deliverResult(galleryMapResult)
        } else {
            forceLoad()
        }
    }

    override fun loadInBackground(): GalleryMapResult {
        try {
            val flickrResponse = callFlickrApi()
            val responseBody = flickrResponse.body()
            if (flickrResponse.isSuccessful && responseBody != null) {
                val resultList = mutableListOf<GalleryMapItem>()
                filteredPhotos(responseBody).forEach { galleryItem ->
                    val bitmap = LocatrApp.photoFetcher.getBitmapByUrl(galleryItem.url)
                    resultList.add(GalleryMapItem(galleryItem, bitmap))
                }
                return successResult(resultList)
            }

        } catch (ioe: IOException) {
            Log.e(TAG, "Cannot load photo", ioe)
        }
        return failureResult()
    }

    private fun successResult(resultList: MutableList<GalleryMapItem>): GalleryMapResult {
        val result = GalleryMapResult(resultList.toList(), true)
        galleryMapResult = result
        return result
    }

    private fun callFlickrApi(): Response<FlickrResponse> {
        return ApiFactory.flickrApi.search(
            location.latitude.toString(),
            location.longitude.toString()
        ).execute()
    }

    private fun filteredPhotos(responseBody: FlickrResponse) =
        responseBody.photos.items.filter { it.url.isNotEmpty() }.distinctBy { it.caption }

    private fun failureResult() = GalleryMapResult(emptyList(), false)


    companion object {
        const val TAG = "PhotosLoader"
    }
}