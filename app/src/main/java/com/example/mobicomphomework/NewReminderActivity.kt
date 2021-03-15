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
import com.example.mobicomphomework.db.ReminderMessage
import java.text.SimpleDateFormat
import java.util.*

class NewReminderActivity : AppCompatActivity(), DatePickerDialog.OnDateSetListener,
        TimePickerDialog.OnTimeSetListener {

    private val databaseTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm")
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy")
    private val timeFormat = SimpleDateFormat("HH:mm")
    private val choiceCalendar = GregorianCalendar.getInstance()
    private var coordinates = doubleArrayOf(UNDEFINED_COORDINATE, UNDEFINED_COORDINATE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_reminder)

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

        findViewById<Button>(R.id.btnCreateReminder).setOnClickListener {
            saveReminder()
        }
    }

    private fun saveReminder() {

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

        val reminderMessage = ReminderMessage(
                null,
                message = message,
                location_x = coordinates[1],
                location_y = coordinates[0],
                reminder_time = databaseTimeFormat.format(choiceCalendar.time),
                creation_time = databaseTimeFormat.format(Calendar.getInstance().time),
                0,false
        )

        AsyncTask.execute {
            // save message to room database
            val db = Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java,
                getString(R.string.databaseFilename)
            ).build()
            val msgid = db.messageDao().insert(reminderMessage).toInt()

            // schedule a notification if the "Show notification" box is checked
            if (findViewById<CheckBox>(R.id.checkBoxNotification).isChecked) {
                if (coordinates[0] == UNDEFINED_COORDINATE) {

                    // schedule a notification at the specified time if location is disabled
                    // and the time of the reminder is in the future
                    if (choiceCalendar.timeInMillis > Calendar.getInstance().timeInMillis) {

                        MessageActivity.setNotification(
                                applicationContext,
                                msgid,
                                choiceCalendar.timeInMillis,
                                reminderMessage.message)
                    }
                }
            } else {
                if (coordinates[0] != UNDEFINED_COORDINATE) {
                    // if a location is set, but notifications are disabled,
                    // tell the application not to show a location based notification
                    db.messageDao().setReminderSeen(msgid)
                }
            }
            db.close()

            // end the reminder creation activity after adding the reminder
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
