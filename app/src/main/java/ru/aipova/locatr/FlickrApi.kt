package ru.aipova.locatr

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface FlickrApi {

    @GET("services/rest")
    fun search(
        @Query("method") method: String = "flickr.photos.search",
        @Query("lat") lat: String,
        @Query("lon") lon: String,
        @Query("api_key") apiKey: String,
        @Query("format") format: String = "json",
        @Query("nojsoncallback") noJsonCallback: String = "1",
        @Query("extras") extras: String = "url_s,geo",
        @Query("per_page") perPage: String = "10"
    ) : Call<FlickrResponse>
}