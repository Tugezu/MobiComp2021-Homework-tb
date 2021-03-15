package com.example.mobicomphomework

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import androidx.room.Room
import com.example.mobicomphomework.db.AppDatabase
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import java.text.SimpleDateFormat
import java.util.*

class GeofenceReceiver: BroadcastReceiver() {
    private lateinit var msgid: String
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm")


    override fun onReceive(context: Context?, intent: Intent?) {
        println("INFO: Geofence entered")
        if (context != null) {
            val geofencingEvent = GeofencingEvent.fromIntent(intent)
            val geofencingTransition = geofencingEvent.geofenceTransition

            if (geofencingTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
                geofencingTransition == Geofence.GEOFENCE_TRANSITION_DWELL) {
                // Retrieve data from intent
                if (intent != null) {
                    msgid = intent.getStringExtra("msgid")!!
                }

                AsyncTask.execute {
                    // save message to room database
                    val db = Room.databaseBuilder(
                            context.applicationContext,
                            AppDatabase::class.java,
                            context.getString(R.string.databaseFilename)
                    ).build()

                    val reminderMessage = db.messageDao().getReminder(msgid.toInt())

                    val reminderCalendar = Calendar.getInstance()
                    reminderCalendar.time = dateFormat.parse(reminderMessage.reminder_time)!!

                    // check if the notification should be shown
                    if (Calendar.getInstance().timeInMillis >= reminderCalendar.timeInMillis) {

                        MessageActivity.showNotification(context.applicationContext,
                                reminderMessage.message)

                        // set the reminder as seen
                        db.messageDao().setReminderSeen(msgid.toInt())

                        // remove the triggering geofence
                        val triggeringGeofences = geofencingEvent.triggeringGeofences
                        MapsActivity.removeGeofences(context.applicationContext, triggeringGeofences)
                    }

                    db.close()
                }
            }
        }
    }
}
