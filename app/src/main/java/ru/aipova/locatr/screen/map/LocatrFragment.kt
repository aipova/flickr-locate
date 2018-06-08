package ru.aipova.locatr.screen.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Location
import android.os.Bundle
import android.support.v4.app.LoaderManager
import android.support.v4.content.ContextCompat
import android.support.v4.content.Loader
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
import ru.aipova.locatr.R
import ru.aipova.locatr.model.GalleryMapItem
import ru.aipova.locatr.model.GalleryMapResult

class LocatrFragment : SupportMapFragment() {
    private lateinit var apiClient: GoogleApiClient
    private var mapItems: List<GalleryMapItem> = listOf()
    private var currentLocation: Location? = null
    private var map: GoogleMap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        restoreLocation(savedInstanceState)
        buildGoogleMap()
        if (currentLocation != null) {
            loadImages()
        }
    }

    private fun restoreLocation(savedInstanceState: Bundle?) {
        savedInstanceState?.run {
            if (containsKey(LOCATION_LAT)) {
                currentLocation = Location("").apply {
                    latitude = getDouble(LOCATION_LAT)
                    longitude = getDouble(LOCATION_LON)
                }
            }
        }
    }

    private fun buildGoogleMap() {
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

    override fun onSaveInstanceState(bundle: Bundle) {
        super.onSaveInstanceState(bundle)
        currentLocation?.run {
            bundle.putDouble(LOCATION_LAT, latitude)
            bundle.putDouble(LOCATION_LON, longitude)
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

    private fun loadImages(restart: Boolean = false) {
        val callbacks =PhotoLoaderCallbacks()
        if (restart) {
            activity?.supportLoaderManager?.restartLoader(R.id.images_loader_id, Bundle.EMPTY, callbacks)
        } else {
            activity?.supportLoaderManager?.initLoader(R.id.images_loader_id, Bundle.EMPTY, callbacks)
        }
    }

    private fun reloadImages() {
        loadImages(true)
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
                currentLocation = location
                Log.i(TAG, "Got a fix: $location")
                reloadImages()
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


    inner class PhotoLoaderCallbacks : LoaderManager.LoaderCallbacks<GalleryMapResult> {
        override fun onCreateLoader(id: Int, args: Bundle?): Loader<GalleryMapResult> {
            return PhotosLoader(activity!!, currentLocation)
        }

        override fun onLoadFinished(loader: Loader<GalleryMapResult>, result: GalleryMapResult) {
            if (result.isSuccessful) {
                mapItems = result.galleryMapItems
                updateUI()
            } else {
                Toast.makeText(activity, "Cannot load photos", Toast.LENGTH_LONG).show()
            }
        }

        override fun onLoaderReset(loader: Loader<GalleryMapResult>) {
        }
    }

    companion object {
        private const val TAG = "LocatrFragment"
        private const val REQUEST_LOCATION_PERMISSIONS = 0
        private const val LOCATION_LAT = "LOCATION_LAT"
        private const val LOCATION_LON = "LOCATION_LON"
        private val LOCATION_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        fun newInstance(): LocatrFragment {
            return LocatrFragment()
        }
    }
}