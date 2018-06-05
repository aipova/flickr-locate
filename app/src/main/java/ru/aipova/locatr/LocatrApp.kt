package ru.aipova.locatr

import android.app.Application
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import ru.aipova.locatr.network.FlickrApi
import ru.aipova.locatr.network.FlickrRepository
import ru.aipova.locatr.network.PhotoFetchr

class LocatrApp : Application() {

    override fun onCreate() {
        super.onCreate()

        val retrofit = Retrofit.Builder().baseUrl(BuildConfig.FLICKR_API_ENDPOINT).addConverterFactory(GsonConverterFactory.create()).build()
        val flickrApi = retrofit.create(FlickrApi::class.java)
        flickrRepository =
                FlickrRepository(flickrApi, BuildConfig.FLICKR_API_KEY)
    }

    companion object {
        lateinit var flickrRepository: FlickrRepository
        var photoFetcher = PhotoFetchr()
    }
}