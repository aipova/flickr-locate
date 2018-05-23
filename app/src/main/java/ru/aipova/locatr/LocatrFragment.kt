package ru.aipova.locatr

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
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
import java.util.*

class LocatrFragment : SupportMapFragment() {
    private lateinit var apiClient: GoogleApiClient
    private var mapImage: Bitmap? = null
    private var mapItem: GalleryItem? = null
    private var mapItems: List<GalleryItem> = listOf()
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
        requestPermissions(LOCATION_PERMISSIONS, REQUEST_LOCATION_PERMISSIONS)
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
                Log.i(TAG, "Got a fix: ${location}")
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
        if (map == null || mapImage == null) {
            return
        }
        val itemPoint = LatLng(mapItem!!.latitude, mapItem!!.longitude)
        val myPoint = LatLng(currentLocation!!.latitude, currentLocation!!.longitude)
        val itemMarker =
            MarkerOptions().position(itemPoint).icon(BitmapDescriptorFactory.fromBitmap(mapImage))
        val myMarker = MarkerOptions().position(myPoint)
        map?.run {
            clear()
            addMarker(itemMarker)
            addMarker(myMarker)
        }
        val bounds = LatLngBounds.Builder()
            .include(itemPoint)
            .include(myPoint)
            .build()
        val margin = resources.getDimensionPixelSize(R.dimen.map_inset_margin)
        val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, margin)
        map?.animateCamera(cameraUpdate)
    }

    inner class SearchTask : AsyncTask<Location, Void, Void>() {
        private var galleryItem: GalleryItem? = null
        private var galleryItems: List<GalleryItem> = listOf()
        private var bitmap: Bitmap? = null
        private lateinit var location: Location

        override fun doInBackground(vararg params: Location): Void? {
            val ctx = this@LocatrFragment.activity?.applicationContext ?: return null
            location = params[0]
            val fetchr = FlickrFetchr(ctx)
            val result = fetchr.searchPhotos(location)
            if (result.isNotEmpty()) {
                galleryItems = result.distinctBy { it.caption }.subList(0, 5)
                galleryItem = result[Random().nextInt(result.size)]
                val bytes = fetchr.getUtlBytes(galleryItem?.url!!)
                bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
            return null
        }

        override fun onPostExecute(result: Void?) {
            mapImage = bitmap
            currentLocation = location
            mapItem = galleryItem
            mapItems = galleryItems
            updateUI()
        }
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