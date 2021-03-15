package com.example.mobicomphomework

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.android.gms.maps.model.LatLng

class DelayedGeofenceCreationWorker(appContext: Context, workerParameters: WorkerParameters) :
        Worker(appContext, workerParameters) {

    override fun doWork(): Result {
        val messageId = inputData.getInt("msgid", 0)
        val coordinates = inputData.getDoubleArray("latLng")!!
        val latLong = LatLng(coordinates[0], coordinates[1])

        MapsActivity.createGeofence(applicationContext, latLong, messageId)
        return Result.success()
    }
}