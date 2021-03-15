package com.example.mobicomphomework

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room
import com.example.mobicomphomework.Constants.CHOOSE_LOCATION_REQUEST_CODE
import com.example.mobicomphomework.Constants.UNDEFINED_COORDINATE
import com.example.mobicomphomework.db.AppDatabase
import java.text.SimpleDateFormat
import java.util.*

class EditReminderActivity : AppCompatActivity(), DatePickerDialog.OnDateSetListener,
        TimePickerDialog.OnTimeSetListener {

    private val databaseTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm")
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy")
    private val timeFormat = SimpleDateFormat("HH:mm")
    private val choiceCalendar = GregorianCalendar.getInstance()
    private var coordinates = doubleArrayOf(UNDEFINED_COORDINATE, UNDEFINED_COORDINATE)

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

        findViewById<TextView>(R.id.editTextLocation).setOnClickListener {
            val intent = Intent(applicationContext, ChooseLocationActivity::class.java).apply {
                putExtra("previousLatLng", coordinates)
            }
            startActivityForResult(intent, CHOOSE_LOCATION_REQUEST_CODE)
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

            // if time is already enabled, display the previously chosen time
            // otherwise, the chosen time should default to the current time
            if (choiceCalendar.timeInMillis > 0) {
                findViewById<TextView>(R.id.editTextTime).text = timeFormat.format(choiceCalendar.time)
                findViewById<TextView>(R.id.editTextDate).text = dateFormat.format(choiceCalendar.time)
            } else {
                choiceCalendar.time = Calendar.getInstance().time
            }

            val creationTimeCalendar = Calendar.getInstance()
            creationTimeCalendar.time = databaseTimeFormat.parse(reminderMessage.creation_time)!!

            coordinates = doubleArrayOf(reminderMessage.location_y, reminderMessage.location_x)

            // display the rest of the values in their appropriate fields
            findViewById<TextView>(R.id.editTextMessage).text = reminderMessage.message
            findViewById<TextView>(R.id.textCreationTime).text = "Created on " +
                    timeFormat.format(creationTimeCalendar.time) + " " +
                    dateFormat.format(creationTimeCalendar.time)

            if (reminderMessage.location_y != UNDEFINED_COORDINATE) {
                findViewById<TextView>(R.id.editTextLocation).text =
                        "Lat: %.${3}f Long: %.${3}f".format(coordinates[0], coordinates[1])
            }
        }

    }

    private fun editReminder(messageId: Int) {

        // cancel any pending notifications
        MessageActivity.cancelNotification(applicationContext, messageId)

        val message = findViewById<TextView>(R.id.editTextMessage).text.toString()
        if (message.isEmpty()) {
            Toast.makeText(applicationContext, "Please enter a message",
                    Toast.LENGTH_SHORT).show()
            return
        }

        // if the set time is not in the future, consider time to be disabled
        if (choiceCalendar.timeInMillis <= Calendar.getInstance().timeInMillis) {
            if (coordinates[0] == UNDEFINED_COORDINATE) {

                // either a time or location has to be defined
                Toast.makeText(applicationContext, "Please define a time or location",
                        Toast.LENGTH_SHORT).show()
                return
            }

            // denote a disabled time with Jan 1st, 1970 0:00 UTC
            choiceCalendar.timeInMillis = 0
        }

        val reminderTime = databaseTimeFormat.format(choiceCalendar.time)

        var reminderSeen = false

        AsyncTask.execute {
            val db = Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java,
                getString(R.string.databaseFilename)
            ).build()

            // schedule a notification if the "Show notification" box is checked
            // and the time of the reminder is in the future
            if (findViewById<CheckBox>(R.id.checkBoxNotification).isChecked) {

                // schedule a notification at the specified time if location is disabled
                // and the time of the reminder is in the future
                if (coordinates[0] == UNDEFINED_COORDINATE) {
                    if (choiceCalendar.timeInMillis > Calendar.getInstance().timeInMillis) {

                        MessageActivity.setNotification(
                                applicationContext,
                                messageId,
                                choiceCalendar.timeInMillis,
                                message)
                    }
                }
            } else {
                if (coordinates[0] != UNDEFINED_COORDINATE) {
                    // if a location is set, but notifications are disabled,
                    // tell the application to not show a location based notification
                    reminderSeen = true
                }
            }

            // update the reminder
            db.messageDao().updateReminder(messageId, message, coordinates[1], coordinates[0],
                    reminderTime, reminderSeen)
            db.close()

            // end the activity after editing the reminder
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (requestCode == CHOOSE_LOCATION_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                coordinates = intent!!.getDoubleArrayExtra("latLng")!!

                if (coordinates[0] != UNDEFINED_COORDINATE) {
                    findViewById<TextView>(R.id.editTextLocation).text =
                            "Lat: %.${3}f Long: %.${3}f".format(coordinates[0], coordinates[1])
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, intent)
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