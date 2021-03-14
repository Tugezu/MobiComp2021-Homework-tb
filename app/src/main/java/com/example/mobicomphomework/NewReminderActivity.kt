package com.example.mobicomphomework

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room
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

        findViewById<TextView>(R.id.editTextTime).text = timeFormat.format(choiceCalendar.time)
        findViewById<TextView>(R.id.editTextDate).text = dateFormat.format(choiceCalendar.time)

        findViewById<TextView>(R.id.editTextLocation).setOnClickListener {
            startActivityForResult(Intent(applicationContext, ChooseLocationActivity::class.java),
                    0)
        }

        findViewById<Button>(R.id.btnCreateReminder).setOnClickListener {
            saveReminder()
        }
    }

    private fun saveReminder() {

        // get current time
        val currentTime = Calendar.getInstance()

        // retrieve the date to be saved
        val reminderCalendar = GregorianCalendar.getInstance()
        val dateParts = findViewById<TextView>(R.id.editTextDate).text.split(".").toTypedArray()
        val timeParts = findViewById<TextView>(R.id.editTextTime).text.split(":").toTypedArray()

        reminderCalendar.set(Calendar.YEAR, dateParts[2].toInt())
        reminderCalendar.set(Calendar.MONTH, dateParts[1].toInt()-1)
        reminderCalendar.set(Calendar.DAY_OF_MONTH, dateParts[0].toInt())
        reminderCalendar.set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
        reminderCalendar.set(Calendar.MINUTE, timeParts[1].toInt())

        val reminderMessage = ReminderMessage(
                null,
                message = findViewById<TextView>(R.id.editTextMessage).text.toString(),
                location_x = coordinates[1],
                location_y = coordinates[0],
                reminder_time = databaseTimeFormat.format(reminderCalendar.time),
                creation_time = databaseTimeFormat.format(currentTime.time),
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
            db.close()

            // schedule a notification if the "Show notification" box is checked
            // and the time of the reminder is in the future
            if (findViewById<CheckBox>(R.id.checkBoxNotification).isChecked) {
                if (reminderCalendar.timeInMillis > Calendar.getInstance().timeInMillis) {

                    MessageActivity.setNotification(
                            applicationContext,
                            msgid,
                            reminderCalendar.timeInMillis,
                            reminderMessage.message)
                }
            }

            // TODO: CHECK IF LOCATION IS EMPTY - ALLOW EITHER TIME OR LOCATION TO BE OMITTED

            // end the reminder creation activity after adding the reminder
            finish()
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (requestCode == 0) {
            if (resultCode == RESULT_OK) {
                coordinates = intent!!.getDoubleArrayExtra("latLng")!!
                findViewById<TextView>(R.id.editTextLocation).text =
                        "Lat: %.${3}f Long: %.${3}f".format(coordinates.get(0), coordinates.get(1))
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
