package com.example.mobicomphomework

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Message
import androidx.room.Room
import com.example.mobicomphomework.db.AppDatabase
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import java.util.*

class GeofenceReceiver: BroadcastReceiver() {
    private lateinit var message: String

    override fun onReceive(context: Context?, intent: Intent?) {
        println("INFO: Geofence entered")
        if (context != null) {
            val geofencingEvent = GeofencingEvent.fromIntent(intent)
            val geofencingTransition = geofencingEvent.geofenceTransition

            if (geofencingTransition == Geofence.GEOFENCE_TRANSITION_ENTER || geofencingTransition == Geofence.GEOFENCE_TRANSITION_DWELL) {
                // Retrieve data from intent
                if (intent != null) {
                    message = intent.getStringExtra("message")!!
                }

                MapsActivity.showNotification(context.applicationContext, message)

                // remove geofence
                val triggeringGeofences = geofencingEvent.triggeringGeofences
                MapsActivity.removeGeofences(context.applicationContext, triggeringGeofences)
            }
        }
    }
}
