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

class EditReminderActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_reminder)

        val messageId = getIntent().getStringExtra("EDIT_REMINDER_ID")!!.toInt()

        findViewById<Button>(R.id.btnConfirmEdit).setOnClickListener {
            editReminder(messageId)
        }

        AsyncTask.execute {
            val db = Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java,
                getString(R.string.databaseFilename)
            ).build()

            // retrieve the message being edited
            val reminderMessage = db.messageDao().getReminder(messageId)
            db.close()

            val timeParts = reminderMessage.reminder_time.split(" ").toTypedArray()

            // display the old values in their appropriate fields
            findViewById<TextView>(R.id.editTextMessage).text = reminderMessage.message
            findViewById<TextView>(R.id.editTextTime).text = timeParts[0]
            findViewById<TextView>(R.id.editTextDate).text = timeParts[1]
            findViewById<TextView>(R.id.textCreationTime).text = "Created on " + reminderMessage.creation_time
        }

    }

    private fun editReminder(messageId: Int) {
        // retrieve the newly entered values
        val message = findViewById<TextView>(R.id.editTextMessage).text.toString()
        val reminderTime = findViewById<TextView>(R.id.editTextTime).text.toString() + " " +
                           findViewById<TextView>(R.id.editTextDate).text.toString()

        AsyncTask.execute {
            val db = Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java,
                getString(R.string.databaseFilename)
            ).build()

            // update the reminder
            db.messageDao().updateReminder(messageId, message, reminderTime)
            db.close()

            Looper.prepare()
            Toast.makeText(
                applicationContext,
                "Reminder updated",
                Toast.LENGTH_SHORT).show()

            // end the activity after editing the reminder
            finish()
        }
    }
}