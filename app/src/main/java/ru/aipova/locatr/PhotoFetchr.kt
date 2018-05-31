package ru.aipova.locatr

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.net.HttpURLConnection
import java.net.URL

class PhotoFetchr {

    fun getBitmapByUrl(urlSpec: String): Bitmap {
        val url = URL(urlSpec)
        val connection = url.openConnection() as HttpURLConnection
        try {
            val bytes =  connection.inputStream.use { it.readBytes(1024) }
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        private val TAG = PhotoFetchr::class.java.name
    }
}