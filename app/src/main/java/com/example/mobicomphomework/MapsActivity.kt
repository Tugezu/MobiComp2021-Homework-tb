package com.example.mobicomphomework

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.room.Room
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.mobicomphomework.Constants.CAMERA_ZOOM_LEVEL
import com.example.mobicomphomework.Constants.GEOFENCE_DWELL_DELAY
import com.example.mobicomphomework.Constants.GEOFENCE_EXPIRATION
import com.example.mobicomphomework.Constants.GEOFENCE_LOCATION_REQUEST_CODE
import com.example.mobicomphomework.Constants.GEOFENCE_RADIUS
import com.example.mobicomphomework.Constants.LOCATION_REQUEST_CODE
import com.example.mobicomphomework.db.AppDatabase
import com.example.mobicomphomework.db.ReminderMessage
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


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

        // zoom to last known location
        fusedLocationClient.lastLocation.addOnSuccessListener {
            if (it != null) {
                with(map) {
                    val latLng = LatLng(it.latitude, it.longitude)
                    moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, CAMERA_ZOOM_LEVEL))
                }
            }
        }

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
                generateGeofences()
            }
        } else {
            generateGeofences()
        }

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

            val reminderMessages = db.messageDao().getUnseenLocationEnabledReminders()
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
                                .strokeColor(Color.argb(50, 50, 50, 100))
                                .fillColor(Color.argb(70, 120, 120, 200))
                                .radius(GEOFENCE_RADIUS.toDouble())
                        )

                        val databaseTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm")
                        val reminderCalendar = Calendar.getInstance()
                        reminderCalendar.time = databaseTimeFormat.parse(reminderMessage.reminder_time)!!

                        if (reminderCalendar.timeInMillis > Calendar.getInstance().timeInMillis) {

                            // If the time of the location enabled reminder is in the future,
                            // the Geofence has to be created at the time of the reminder.
                            // Otherwise the Geofence will not trigger if the user happens to be
                            // inside the Geofence at the time of the reminder.

                            val geofenceParameters = Data.Builder()
                                    .putInt("msgid", reminderMessage.msgid!!)
                                    .putDoubleArray("latLng",
                                            doubleArrayOf(reminderMessage.location_y,
                                                          reminderMessage.location_x))
                                    .build()

                            // get the time from now until the reminder time
                            val timeFromNow = reminderCalendar.timeInMillis - System.currentTimeMillis()

                            // schedule the Geofence to be created at the time of the reminder
                            val geofenceWorkRequest = OneTimeWorkRequestBuilder<DelayedGeofenceCreationWorker>()
                                    .setInputData(geofenceParameters)
                                    .setInitialDelay(timeFromNow, TimeUnit.MILLISECONDS)
                                    .addTag(reminderMessage.msgid.toString())
                                    .build()

                            WorkManager.getInstance(applicationContext).enqueue(geofenceWorkRequest)
                        } else {
                            WorkManager.getInstance(applicationContext).cancelAllWorkByTag(reminderMessage.msgid.toString())
                            createGeofence(applicationContext, location, reminderMessage.msgid!!)
                        }
                    }
                }
            }
        }
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
                map.isMyLocationEnabled = true
                onMapReady(map)
            } else {
                Toast.makeText(
                    this,
                    "Location permissions must be granted to enable location based reminders",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        if (requestCode == GEOFENCE_LOCATION_REQUEST_CODE) {
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


    companion object {
        fun createGeofence(context: Context, location: LatLng, messageId: Int) {
            val geofence = Geofence.Builder()
                    .setRequestId(messageId.toString())
                    .setCircularRegion(location.latitude, location.longitude, GEOFENCE_RADIUS.toFloat())
                    .setExpirationDuration(GEOFENCE_EXPIRATION.toLong())
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_DWELL)
                    .setLoiteringDelay(GEOFENCE_DWELL_DELAY)
                    .build()

            val geofenceRequest = GeofencingRequest.Builder()
                    .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                    .addGeofence(geofence)
                    .build()

            val pendingIntent: PendingIntent by lazy {
                val intent = Intent(context, GeofenceReceiver::class.java)
                        .putExtra("msgid", messageId.toString())

                PendingIntent.getBroadcast(context, messageId, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT)
            }


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (ContextCompat.checkSelfPermission(context.applicationContext,
                                Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED) {
                    LocationServices.getGeofencingClient(context).addGeofences(geofenceRequest, pendingIntent)
                    println("INFO: Geofence created")
                }
            } else {
                LocationServices.getGeofencingClient(context).addGeofences(geofenceRequest, pendingIntent)
                println("INFO: Geofence created")
            }
        }

        fun removeGeofences(context: Context, triggeringGeofenceList: MutableList<Geofence>) {
            val geofenceIdList = mutableListOf<String>()
            for (entry in triggeringGeofenceList) {
                geofenceIdList.add(entry.requestId)
            }
            LocationServices.getGeofencingClient(context).removeGeofences(geofenceIdList)
        }
    }
    
}