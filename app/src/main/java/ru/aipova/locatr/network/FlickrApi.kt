package ru.aipova.locatr.network

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query
import ru.aipova.locatr.BuildConfig
import ru.aipova.locatr.model.FlickrResponse

interface FlickrApi {

    @GET("services/rest?method=flickr.photos.search&format=json&nojsoncallback=1&extras=url_s,geo")
    fun search(
        @Query("lat") lat: String,
        @Query("lon") lon: String,
        @Query("api_key") apiKey: String = BuildConfig.FLICKR_API_KEY,
        @Query("per_page") perPage: String = "10"
    ) : Call<FlickrResponse>
}