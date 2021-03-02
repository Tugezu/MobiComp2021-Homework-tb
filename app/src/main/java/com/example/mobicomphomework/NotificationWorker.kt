package com.example.mobicomphomework

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters


class NotificationWorker(appContext: Context, workerParameters: WorkerParameters) :
        Worker(appContext, workerParameters) {

    override fun doWork(): Result {
        val text = inputData.getString("message")
        MessageActivity.showNotification(applicationContext, text!!)
        return Result.success()
    }
}