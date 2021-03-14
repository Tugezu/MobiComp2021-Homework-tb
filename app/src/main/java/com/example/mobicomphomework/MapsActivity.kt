package com.example.mobicomphomework

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.AsyncTask
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.room.Room
import com.example.mobicomphomework.Constants.CAMERA_ZOOM_LEVEL
import com.example.mobicomphomework.Constants.GEOFENCE_DWELL_DELAY
import com.example.mobicomphomework.Constants.GEOFENCE_EXPIRATION
import com.example.mobicomphomework.Constants.GEOFENCE_ID
import com.example.mobicomphomework.Constants.GEOFENCE_LOCATION_REQUEST_CODE
import com.example.mobicomphomework.Constants.GEOFENCE_RADIUS
import com.example.mobicomphomework.Constants.LOCATION_REQUEST_CODE
import com.example.mobicomphomework.db.AppDatabase
import com.example.mobicomphomework.db.ReminderMessage
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.CircleOptions

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

private val TAG: String = MapsActivity::class.java.simpleName

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var geofencingClient: GeofencingClient


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        geofencingClient = LocationServices.getGeofencingClient(this)
    }


    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.uiSettings.isZoomControlsEnabled = true


        if (ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this,Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    LOCATION_REQUEST_CODE
            )
        } else {
            this.map.isMyLocationEnabled = true
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(applicationContext,
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                        Constants.BACKGROUND_LOCATION_REQUEST_CODE)
            }
        }

        // Zoom to last known location
        fusedLocationClient.lastLocation.addOnSuccessListener {
            if (it != null) {
                with(map) {
                    val latLng = LatLng(it.latitude, it.longitude)
                    moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, CAMERA_ZOOM_LEVEL))
                }
            } else {
                with(map) {
                    moveCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                    LatLng(65.0, 25.0),
                                    CAMERA_ZOOM_LEVEL
                            )
                    )
                }
            }
        }

        generateGeofences()
    }

    private fun generateGeofences() {
        LoadReminderMessages().execute()
    }

    inner class LoadReminderMessages : AsyncTask<String?, String?, List<ReminderMessage>>() {
        override fun doInBackground(vararg params: String?): List<ReminderMessage> {
            val db = Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java,
                getString(R.string.databaseFilename)
            ).build()

            val reminderMessages = db.messageDao().getAllReminders()
            db.close()

            return reminderMessages
        }

        override fun onPostExecute(reminderMessages: List<ReminderMessage>?) {
            super.onPostExecute(reminderMessages)
            if (reminderMessages != null) {
                if (reminderMessages.isNotEmpty()) {
                    for (reminderMessage in reminderMessages) {
                        val location = LatLng(
                            reminderMessage.location_y,
                            reminderMessage.location_x
                        )
                        map.addMarker(
                            MarkerOptions()
                                .position(location)
                        ).showInfoWindow()
                        map.addCircle(
                            CircleOptions()
                                .center(location)
                                .strokeColor(Color.argb(50, 70, 70, 70))
                                .fillColor(Color.argb(70, 150, 150, 150))
                                .radius(200.0)
                        )

                        createGeoFence(location, reminderMessage.message, geofencingClient)
                    }
                }
            }
        }
    }

    private fun createGeoFence(location: LatLng, message: String, geofencingClient: GeofencingClient) {
        val geofence = Geofence.Builder()
            .setRequestId(GEOFENCE_ID)
            .setCircularRegion(location.latitude, location.longitude, GEOFENCE_RADIUS.toFloat())
            .setExpirationDuration(GEOFENCE_EXPIRATION.toLong())
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_DWELL)
            .setLoiteringDelay(GEOFENCE_DWELL_DELAY)
            .build()

        val geofenceRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()


        val intent = Intent(this, GeofenceReceiver::class.java)
            .putExtra("message", message)

        val pendingIntent = PendingIntent.getBroadcast(
            applicationContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(
                    applicationContext, Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                    GEOFENCE_LOCATION_REQUEST_CODE
                )
            } else {
                geofencingClient.addGeofences(geofenceRequest, pendingIntent)
                println("INFO: Geofence created")
            }
        } else {
            geofencingClient.addGeofences(geofenceRequest, pendingIntent)
            println("INFO: Geofence created")
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == GEOFENCE_LOCATION_REQUEST_CODE) {
            if (permissions.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(
                    this,
                    "This application needs background location to work on Android 10 and higher",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
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
                map.isMyLocationEnabled = true
                onMapReady(map)
            } else {
                Toast.makeText(
                    this,
                    "The app needs location permission to function",
                    Toast.LENGTH_LONG
                ).show()
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (grantResults.isNotEmpty() && grantResults[2] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(
                        this,
                        "This application needs background location to work on Android 10 and higher",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    companion object {
        fun removeGeofences(context: Context, triggeringGeofenceList: MutableList<Geofence>) {
            val geofenceIdList = mutableListOf<String>()
            for (entry in triggeringGeofenceList) {
                geofenceIdList.add(entry.requestId)
            }
            LocationServices.getGeofencingClient(context).removeGeofences(geofenceIdList)
        }

        fun showNotification(context: Context?, message: String) {
            val CHANNEL_ID = "REMINDER_NOTIFICATION_CHANNEL"
            var notificationId = 1589
            notificationId += Random(notificationId).nextInt(1, 30)

            val notificationBuilder = NotificationCompat.Builder(context!!.applicationContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.alarm_24px)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(message)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(message)
                )
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.app_name),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = context.getString(R.string.app_name)
                }
                notificationManager.createNotificationChannel(channel)
            }
            notificationManager.notify(notificationId, notificationBuilder.build())
        }
    }
    
}