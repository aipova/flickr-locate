package ru.aipova.locatr.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import ru.aipova.locatr.BuildConfig

object ApiFactory {

        val flickrApi: FlickrApi = buildRetrofit().create(FlickrApi::class.java)

        private fun buildRetrofit() =
            Retrofit.Builder().baseUrl(BuildConfig.FLICKR_API_ENDPOINT)
                .client(buildClient())
                .addConverterFactory(GsonConverterFactory.create())
                .build()

        private fun buildClient() =
            OkHttpClient.Builder()
                .addInterceptor(HttpLoggingInterceptor())
                .addInterceptor(ApiKeyInterceptor())
                .build()

}