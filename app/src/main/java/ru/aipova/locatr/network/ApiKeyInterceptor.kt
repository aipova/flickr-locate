package ru.aipova.locatr.network

import okhttp3.Interceptor
import okhttp3.Response
import ru.aipova.locatr.BuildConfig

class ApiKeyInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        val url = request.url().newBuilder()
            .addQueryParameter(API_KEY_PARAM, BuildConfig.FLICKR_API_KEY)
            .build()
        request = request.newBuilder().url(url).build()
        return chain.proceed(request)
    }

    companion object {
        const val API_KEY_PARAM = "api_ley"
    }
}