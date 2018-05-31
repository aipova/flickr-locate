package ru.aipova.locatr

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName

data class FlickrResponse(val photos: GalleryResult, val stat: String)

data class GalleryResult(
    val page: Int,
    val pages: Int,
    val perpage: Int,
    val total: Int, @SerializedName("photo") val items: List<GalleryItem>
)

data class GalleryItem(
    val id: String,
    @SerializedName("title") val caption: String,
    @SerializedName("url_s") val url: String?,
    val owner: String,
    val latitude: Double,
    val longitude: Double) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readDouble(),
        parcel.readDouble()
    )

    override fun toString(): String {
        return caption.toUpperCase()
    }

    override fun equals(other: Any?): Boolean {
        return other is GalleryItem && other.id == id
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(caption)
        parcel.writeString(url)
        parcel.writeString(owner)
        parcel.writeDouble(latitude)
        parcel.writeDouble(longitude)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<GalleryItem> {
        override fun createFromParcel(parcel: Parcel): GalleryItem {
            return GalleryItem(parcel)
        }

        override fun newArray(size: Int): Array<GalleryItem?> {
            return arrayOfNulls(size)
        }
    }
}