package ru.aipova.locatr.screen.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context.CONNECTIVITY_SERVICE
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Location
import android.net.ConnectivityManager
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import ru.aipova.locatr.LocatrApp
import ru.aipova.locatr.R
import ru.aipova.locatr.model.GalleryItem
import ru.aipova.locatr.model.GalleryMapItem
import ru.aipova.locatr.network.ApiFactory
import java.io.IOException

class LocatrFragment : SupportMapFragment() {
    private lateinit var apiClient: GoogleApiClient
    private var mapItems: List<GalleryMapItem> = listOf()
    private var currentLocation: Location? = null
    private var map: GoogleMap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        apiClient = GoogleApiClient.Builder(activity!!)
            .addApi(LocationServices.API)
            .addConnectionCallbacks(object : GoogleApiClient.ConnectionCallbacks {
                override fun onConnected(p0: Bundle?) {
                    activity?.invalidateOptionsMenu()
                }

                override fun onConnectionSuspended(p0: Int) {
                }
            })
            .build()
        getMapAsync { googleMap ->
            map = googleMap
            updateUI()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?) = when (item?.itemId) {
        R.id.action_locate -> {
            if (hasLocationPermissions()) {
                findImage()
            } else {
                if (shouldShowRequestPermissionRationale(LOCATION_PERMISSIONS[0])) {
                    AlertDialog.Builder(activity!!)
                        .setMessage(R.string.permission)
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.ok, { di, i -> requestPermission() })
                        .show()
                } else {
                    requestPermission()
                }
            }
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun requestPermission() {
        requestPermissions(
            LOCATION_PERMISSIONS,
            REQUEST_LOCATION_PERMISSIONS
        )
    }

    override fun onStart() {
        super.onStart()

        activity?.invalidateOptionsMenu()
        apiClient.connect()
    }

    override fun onStop() {
        super.onStop()
        apiClient.disconnect()
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater?.inflate(R.menu.fragment_locatr, menu)
        menu?.findItem(R.id.action_locate)?.isEnabled = apiClient.isConnected
    }


    @SuppressLint("MissingPermission")
    private fun findImage() {
        val request = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            numUpdates = 1
            interval = 0
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(
            apiClient,
            request,
            { location ->
                Log.i(TAG, "Got a fix: $location")
                SearchTask().execute(location)
            })
    }

    private fun hasLocationPermissions(): Boolean {
        val result = ContextCompat.checkSelfPermission(activity!!, LOCATION_PERMISSIONS[0])
        return result == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_LOCATION_PERMISSIONS -> {
                if (hasLocationPermissions()) {
                    findImage()
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun updateUI() {
        if (map == null || mapItems.isEmpty()) {
            return
        }
        val itemMarkers = mutableListOf<MarkerOptions>()
        val boundsBuilder = LatLngBounds.Builder()
        mapItems.forEach { galleryMapItem ->
            val mapItem = galleryMapItem.galleryItem
            val mapImage = galleryMapItem.mapImage
            val itemPoint = LatLng(mapItem.latitude, mapItem.longitude)
            itemMarkers.add(createMarker(itemPoint, mapImage))
            boundsBuilder.include(itemPoint)
        }

        val myPoint = LatLng(currentLocation!!.latitude, currentLocation!!.longitude)
        val myMarker = MarkerOptions().position(myPoint)
        val bounds = boundsBuilder.include(myPoint).build()

        map?.run {
            clear()
            itemMarkers.forEach { addMarker(it) }
            addMarker(myMarker)
        }

        val margin = resources.getDimensionPixelSize(R.dimen.map_inset_margin)
        val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, margin)
        map?.animateCamera(cameraUpdate)
    }

    private fun createMarker(
        itemPoint: LatLng,
        mapImage: Bitmap
    ) = MarkerOptions().position(itemPoint).icon(BitmapDescriptorFactory.fromBitmap(mapImage))

    inner class SearchTask : AsyncTask<Location, Void, SearchResult>() {
        private var galleryBitmapItems: MutableList<GalleryMapItem> = mutableListOf()
        private var location: Location? = null

        override fun doInBackground(vararg params: Location): SearchResult {
            val connectivityManager = activity?.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkInfo = connectivityManager.activeNetworkInfo
            if (networkInfo == null || !networkInfo.isConnected) {
                return SearchResult.NOT_OK
            }
            location = params[0]
            try {
                val result = getFlickrResult(params[0])
                if (result.isNotEmpty()) {
                    val galleryItems = result.filter { it.url.isNotEmpty() }.distinctBy { it.caption }
                    galleryItems.forEach { galleryItem ->
                            val bitmap = LocatrApp.photoFetcher.getBitmapByUrl(galleryItem.url)
                            galleryBitmapItems.add(
                                GalleryMapItem(
                                    galleryItem,
                                    bitmap
                                )
                            )

                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Cannot load flickr photos", e)
                return SearchResult.NOT_OK
            }
            return SearchResult.OK
        }

        fun getFlickrResult(location: Location): List<GalleryItem> {
            val resultBody = ApiFactory.flickrApi.search(location.latitude.toString(), location.longitude.toString()).execute().body()
            if (resultBody != null) {
                return resultBody.photos.items
            } else {
                return listOf()
            }
        }

        override fun onPostExecute(result: SearchResult) {
            if (result == SearchResult.NOT_OK) {
                Toast.makeText(activity, "Cannot load photos, check internet connection", Toast.LENGTH_LONG).show()
            } else {
                currentLocation = location
                mapItems = galleryBitmapItems
                updateUI()
            }
        }
    }

    enum class SearchResult {
        OK, NOT_OK
    }

    companion object {
        private const val TAG = "LocatrFragment"
        private const val REQUEST_LOCATION_PERMISSIONS = 0
        private val LOCATION_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        fun newInstance(): LocatrFragment {
            return LocatrFragment()
        }
    }
}