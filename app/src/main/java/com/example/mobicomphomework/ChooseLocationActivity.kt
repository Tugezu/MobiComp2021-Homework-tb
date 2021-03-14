package com.example.mobicomphomework

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.mobicomphomework.Constants.BACKGROUND_LOCATION_REQUEST_CODE
import com.example.mobicomphomework.Constants.CAMERA_ZOOM_LEVEL
import com.example.mobicomphomework.Constants.LOCATION_REQUEST_CODE
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import java.util.*


class ChooseLocationActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var chosenLocation: LatLng

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose_location)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true

        if (ContextCompat.checkSelfPermission(
                        this, Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                        this, Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION),
                    LOCATION_REQUEST_CODE)
        } else {
            mMap.isMyLocationEnabled = true
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(applicationContext,
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                        BACKGROUND_LOCATION_REQUEST_CODE)
            }
        }

        // TODO: initialize to current ("virtual") location
        // Zoom to last known location
        fusedLocationClient.lastLocation.addOnSuccessListener {
            if (it != null) {
                with(mMap) {
                    val latLng = LatLng(it.latitude, it.longitude)
                    moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, CAMERA_ZOOM_LEVEL))
                }
            } else {
                with(mMap) {
                    moveCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                    LatLng(65.01355297927051, 25.464019811372978),
                                    CAMERA_ZOOM_LEVEL
                            )
                    )
                }
            }
        }

        setMapCLickListener(mMap)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.map_confirm, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        confirmChosenLocation()
        return true
    }

    private fun setMapCLickListener(map: GoogleMap) {
        map.setOnMapClickListener {
            val snippet = String.format(
                    Locale.getDefault(),
                    "Lat: %1$.5f, Long: %2$.5f",
                    it.latitude,
                    it.longitude
            )

            map.moveCamera(CameraUpdateFactory.newLatLngZoom(it, CAMERA_ZOOM_LEVEL))

            // remove old markers
            map.clear()

            // add a marker to the selected location
            map.addMarker(
                    MarkerOptions()
                            .position(it)
                            .title("Chosen location")
                            .snippet(snippet)
            ).showInfoWindow()
            map.addCircle(
                    CircleOptions()
                            .center(it)
                            .strokeColor(Color.argb(50, 70, 70, 70))
                            .fillColor(Color.argb(70, 150, 150, 150))
                            .radius(200.0)
            )

            // update chosen location
            chosenLocation = it
        }
    }

    private fun confirmChosenLocation() {
        val intent = Intent().apply {
            putExtra("latLng",
                    doubleArrayOf(chosenLocation.latitude, chosenLocation.longitude))
        }
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        if (requestCode == LOCATION_REQUEST_CODE) {
            if (
                    grantResults.isNotEmpty() && (
                            grantResults[0] == PackageManager.PERMISSION_GRANTED ||
                                    grantResults[1] == PackageManager.PERMISSION_GRANTED)
            ) {
                if (ActivityCompat.checkSelfPermission(
                                this,
                                Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                                this,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return
                }
                mMap.isMyLocationEnabled = true
                onMapReady(mMap)
            } else {
                Toast.makeText(
                        this,
                        "Location permissions must be granted to enable location based reminders",
                        Toast.LENGTH_LONG
                ).show()
            }
        }

        if (requestCode == BACKGROUND_LOCATION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(
                        this,
                        "Please enable background location to use location based reminders on" +
                                "Android 10 and higher",
                        Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}