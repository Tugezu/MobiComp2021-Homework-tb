package com.example.mobicomphomework

import android.os.AsyncTask
import android.os.Bundle
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room
import com.example.mobicomphomework.db.AppDatabase
import com.example.mobicomphomework.db.ReminderMessage
import java.util.*

class NewReminderActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_reminder)

        findViewById<Button>(R.id.btnCreateReminder).setOnClickListener {
            saveReminder()
        }
    }

    private fun saveReminder() {
        val currentTime = Calendar.getInstance()
        val reminderMessage = ReminderMessage(
            null,
            message = findViewById<TextView>(R.id.editTextMessage).text.toString(),
            location_x = 0.0,
            location_y = 0.0,
            reminder_time = findViewById<TextView>(R.id.editTextTime).text.toString() + " " +
                            findViewById<TextView>(R.id.editTextDate).text.toString(),
            creation_time = currentTime.get(Calendar.HOUR_OF_DAY).toString() + ":" +
                            String.format("%02d", currentTime.get(Calendar.MINUTE)) + " " +
                            currentTime.get(Calendar.DATE).toString() + "." +
                            currentTime.get(Calendar.MONTH).toString() + "." +
                            currentTime.get(Calendar.YEAR).toString(),
            0,false
        )

        AsyncTask.execute {
            // save message to room database
            val db = Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java,
                getString(R.string.databaseFilename)
            ).build()
            db.messageDao().insert(reminderMessage)
            db.close()

            Looper.prepare()
            Toast.makeText(
                applicationContext,
                "Reminder added",
                Toast.LENGTH_SHORT).show()

            // end the reminder creation activity after adding the reminder
            finish()
        }

    }

}
