package com.example.mobicomphomework

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.*
import android.widget.AdapterView
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.room.Room
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.mobicomphomework.db.AppDatabase
import com.example.mobicomphomework.db.ReminderMessage
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class MessageActivity : AppCompatActivity() {
    private lateinit var listView: ListView
    lateinit var updateHandler: Handler
    private var showAll = false

    private val updateRemindersTask = object : Runnable {
        override  fun run() {
            updateListView()
            updateHandler.postDelayed(this, 15000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message)

        listView = findViewById(R.id.reminderMsgListView)

        listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->

            // retrieve the selected Item
            val selectedReminder = listView.adapter.getItem(position) as ReminderMessage

            // show an AlertDialog with edit options
            val builder = AlertDialog.Builder(this@MessageActivity)
            builder.setTitle("Edit reminder")
                    .setMessage("Would you like to edit or delete this reminder?")
                    .setPositiveButton("Edit") { _, _ ->

                        intent = Intent(applicationContext, EditReminderActivity::class.java)

                        // pass the id of the message to edit to the edit activity
                        intent.putExtra("EDIT_REMINDER_ID",
                                selectedReminder.msgid.toString())

                        // pass the notification status
                        intent.putExtra("NOTIFICATION_ENABLED", checkNotificationStatus(
                                selectedReminder.msgid!!,
                                selectedReminder.location_y !=
                                        Constants.UNDEFINED_COORDINATE,
                                selectedReminder.reminder_seen).toString())

                        // start the reminder edit activity
                        startActivity(intent)

                        // update the list view
                        updateListView()
                    }
                    .setNegativeButton("Delete") { _, _ ->
                        // remove the reminder from the database
                        AsyncTask.execute {
                            val db = Room
                                    .databaseBuilder(
                                            applicationContext,
                                            AppDatabase::class.java,
                                            getString(R.string.databaseFilename)
                                    )
                                    .build()
                            db.messageDao().delete(selectedReminder.msgid!!)
                            db.close()
                        }
                        // cancel the pending notification
                        cancelNotification(applicationContext, selectedReminder.msgid!!)

                        // update the list view
                        updateListView()
                    }
                    .setNeutralButton("Cancel") { dialog, _ -> dialog.dismiss() }
                    .show()

        }

        findViewById<Button>(R.id.btnLogout).setOnClickListener { logOut() }

        findViewById<FloatingActionButton>(R.id.btnNewMessage).setOnClickListener {
            startActivity(Intent(applicationContext, NewReminderActivity::class.java))
        }

        findViewById<Button>(R.id.btnShowAll).setOnClickListener {
            showAll = !showAll

            if (showAll) { findViewById<Button>(R.id.btnShowAll).text = "Show less" }
            else { findViewById<Button>(R.id.btnShowAll).text = "Show all" }

            updateListView()
        }

        findViewById<Button>(R.id.btnShowMap).setOnClickListener {
            startActivity(Intent(applicationContext, MapsActivity::class.java))
        }

        // set updateListView to run every 15 s to check for new reminders
        updateHandler = Handler(Looper.getMainLooper())
        updateHandler.post(updateRemindersTask)

        updateListView()
    }

    private fun checkNotificationStatus(messageId: Int,
                                        locationEnabled: Boolean,
                                        reminderSeen: Boolean): Boolean {
        // checks if notifications are currently enabled for a given message

        // if location is enabled
        if (locationEnabled) {
            return !reminderSeen
        }

        // if location is disabled, check for scheduled notifications
        val future = WorkManager.getInstance(applicationContext)
            .getWorkInfosByTag(messageId.toString())
        val scheduledWork = future.get()

        if ( !((scheduledWork == null) || (scheduledWork.size == 0)) ) {
            for (workInfo in scheduledWork) {
                if (workInfo.state == WorkInfo.State.BLOCKED || workInfo.state ==
                    WorkInfo.State.ENQUEUED || workInfo.state == WorkInfo.State.RUNNING) {
                    return true
                }
            }
        }
        // if no active work tagged with the message id was found, return false
        return false
    }

    private fun logOut() {
        // update login status
        applicationContext.getSharedPreferences(getString(R.string.sharedPrefKey),
                Context.MODE_PRIVATE).edit().putInt("LoginStatus", 0).apply()

        // return to start screen
        startActivity(Intent(applicationContext, MainActivity::class.java))
        finish()

    }

    override fun onPause() {
        super.onPause()
        updateHandler.removeCallbacks(updateRemindersTask)
    }

    override fun onResume() {
        super.onResume()
        updateListView()
        updateHandler.post(updateRemindersTask)
    }

    private fun updateListView() {
        var refreshTask = LoadReminderMessages()
        refreshTask.execute()
    }

    inner class LoadReminderMessages : AsyncTask<String?, String?, List<ReminderMessage>>() {
        override fun doInBackground(vararg params: String?): List<ReminderMessage> {
            val db = Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java,
                getString(R.string.databaseFilename)
            ).build()

            val reminderMessages: List<ReminderMessage>
            if (showAll) {
                reminderMessages = db.messageDao().getAllReminders()
            } else {
                val currentCalendar = Calendar.getInstance()
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm")
                val currentTime = dateFormat.format(currentCalendar.time)
                reminderMessages = db.messageDao().getReminderHistory(currentTime)
            }
            db.close()

            return reminderMessages
        }

        override fun onPostExecute(reminderMessages: List<ReminderMessage>?) {
            super.onPostExecute(reminderMessages)
            if (reminderMessages != null) {
                if (reminderMessages.isNotEmpty()) {
                    val adapter = MessageAdapter(applicationContext, reminderMessages)
                    listView.adapter = adapter
                } else {
                    listView.adapter = null
                    Toast.makeText(applicationContext, "No messages to show", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    companion object {
        fun showNotification(context: Context, message: String) {

            val CHANNEL_ID = "MC_HW_NOTIFICATION_CHANNEL"
            val notificationId = Random.nextInt(10, 1000) + 5

            val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.alarm_24px)
                    .setContentTitle(context.getString(R.string.app_name))
                    .setContentText(message)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setGroup(CHANNEL_ID)

            val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

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

        fun setNotification(
                context: Context,
                msgid: Int,
                timeInMillis: Long,
                message: String
        ) {

            val reminderParameters = Data.Builder()
                    .putString("message", message)
                    .putInt("msgid", msgid)
                    .build()

            // get the time from now until the reminder
            var timeFromNow = 0L
            if (timeInMillis > System.currentTimeMillis())
                timeFromNow = timeInMillis - System.currentTimeMillis()

            val notificationRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
                    .setInputData(reminderParameters)
                    .setInitialDelay(timeFromNow, TimeUnit.MILLISECONDS)
                    .addTag(msgid.toString())
                    .build()

            WorkManager.getInstance(context).enqueue(notificationRequest)
        }

        fun cancelNotification(context: Context, msgid: Int) {
            WorkManager.getInstance(context).cancelAllWorkByTag(msgid.toString())
        }
    }
}