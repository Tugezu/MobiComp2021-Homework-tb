package com.example.mobicomphomework

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.AsyncTask
import android.os.Bundle
import android.text.InputType
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room
import com.example.mobicomphomework.db.AppDatabase
import java.text.SimpleDateFormat
import java.util.*

class EditReminderActivity : AppCompatActivity(), DatePickerDialog.OnDateSetListener,
        TimePickerDialog.OnTimeSetListener {

    private val databaseTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm")
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy")
    private val timeFormat = SimpleDateFormat("HH:mm")
    private val choiceCalendar = GregorianCalendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_reminder)

        val messageId = getIntent().getStringExtra("EDIT_REMINDER_ID")!!.toInt()
        val notificationEnabled = getIntent().getStringExtra("NOTIFICATION_ENABLED")!!.toBoolean()

        findViewById<Button>(R.id.btnConfirmEdit).setOnClickListener {
            editReminder(messageId)
        }

        if (notificationEnabled) {
            findViewById<TextView>(R.id.textNotificationStatus).text = "Notifications currently enabled"
            findViewById<CheckBox>(R.id.checkBoxNotification).isChecked = true
        } else {
            findViewById<TextView>(R.id.textNotificationStatus).text = "Notifications currently disabled"
        }

        val editTextDate = findViewById<TextView>(R.id.editTextDate)
        val editTextTime = findViewById<TextView>(R.id.editTextTime)

        editTextDate.setOnClickListener {
            DatePickerDialog(
                this,
                this,
                choiceCalendar.get(Calendar.YEAR),
                choiceCalendar.get(Calendar.MONTH),
                choiceCalendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        editTextTime.setOnClickListener {
            TimePickerDialog(
                this,
                this,
                choiceCalendar.get(Calendar.HOUR_OF_DAY),
                choiceCalendar.get(Calendar.MINUTE),
                true
            ).show()
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

            choiceCalendar.time = databaseTimeFormat.parse(reminderMessage.reminder_time)!!

            val creationTimeCalendar = Calendar.getInstance()
            creationTimeCalendar.time = databaseTimeFormat.parse(reminderMessage.creation_time)!!

            // display the old values in their appropriate fields
            findViewById<TextView>(R.id.editTextMessage).text = reminderMessage.message
            findViewById<TextView>(R.id.editTextTime).text = timeFormat.format(choiceCalendar.time)
            findViewById<TextView>(R.id.editTextDate).text = dateFormat.format(choiceCalendar.time)
            findViewById<TextView>(R.id.textCreationTime).text = "Created on " +
                    timeFormat.format(creationTimeCalendar.time) + " " +
                    dateFormat.format(creationTimeCalendar.time)
        }

    }

    private fun editReminder(messageId: Int) {

        // cancel the old notification
        MessageActivity.cancelNotification(applicationContext, messageId)

        // retrieve the newly entered values
        val reminderCalendar = GregorianCalendar.getInstance()
        val dateParts = findViewById<TextView>(R.id.editTextDate).text.split(".").toTypedArray()
        val timeParts = findViewById<TextView>(R.id.editTextTime).text.split(":").toTypedArray()

        reminderCalendar.set(Calendar.YEAR, dateParts[2].toInt())
        reminderCalendar.set(Calendar.MONTH, dateParts[1].toInt()-1)
        reminderCalendar.set(Calendar.DAY_OF_MONTH, dateParts[0].toInt())
        reminderCalendar.set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
        reminderCalendar.set(Calendar.MINUTE, timeParts[1].toInt())

        val message = findViewById<TextView>(R.id.editTextMessage).text.toString()
        val reminderTime = databaseTimeFormat.format(reminderCalendar.time)

        AsyncTask.execute {
            val db = Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java,
                getString(R.string.databaseFilename)
            ).build()

            // update the reminder
            db.messageDao().updateReminder(messageId, message, reminderTime)
            db.close()

            // schedule a notification if the "Show notification" box is checked
            // and the time of the reminder is in the future
            if (findViewById<CheckBox>(R.id.checkBoxNotification).isChecked) {
                if (reminderCalendar.timeInMillis > Calendar.getInstance().timeInMillis) {

                    MessageActivity.setNotification(
                            applicationContext,
                            messageId,
                            reminderCalendar.timeInMillis,
                            message)
                }
            }

            // end the activity after editing the reminder
            finish()
        }
    }

    override fun onDateSet(view: DatePicker?, year: Int, month: Int, dayOfMonth: Int) {
        choiceCalendar.set(Calendar.YEAR, year)
        choiceCalendar.set(Calendar.MONTH, month)
        choiceCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
        findViewById<TextView>(R.id.editTextDate).text = dateFormat.format(choiceCalendar.time)
    }

    override fun onTimeSet(view: TimePicker?, hourOfDay: Int, minute: Int) {
        choiceCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
        choiceCalendar.set(Calendar.MINUTE, minute)
        findViewById<TextView>(R.id.editTextTime).text = timeFormat.format(choiceCalendar.time)
    }
}