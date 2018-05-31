package ru.aipova.locatr

import android.app.Application
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class LocatrApp : Application() {

    override fun onCreate() {
        super.onCreate()

        val retrofit = Retrofit.Builder().baseUrl(API_PATH).addConverterFactory(GsonConverterFactory.create()).build()
        val flickrApi = retrofit.create(FlickrApi::class.java)
        flickrRepository = FlickrRepository(flickrApi, getString(R.string.flickr_key))
    }

    companion object {
        private const val API_PATH = "https://api.flickr.com/"
        lateinit var flickrRepository: FlickrRepository
        var photoFetcher = PhotoFetchr()
    }
}