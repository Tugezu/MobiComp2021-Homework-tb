package com.example.mobicomphomework

import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.widget.AdapterView
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room
import com.example.mobicomphomework.db.AppDatabase
import com.example.mobicomphomework.db.ReminderMessage
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MessageActivity : AppCompatActivity() {
    private lateinit var listView: ListView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message)

        listView = findViewById<ListView>(R.id.reminderMsgListView)

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
                        intent.putExtra("EDIT_REMINDER_ID", selectedReminder.msgid.toString())

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
                        }

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

        updateListView()
    }

    private fun logOut() {
        // update login status
        applicationContext.getSharedPreferences(getString(R.string.sharedPrefKey),
                Context.MODE_PRIVATE).edit().putInt("LoginStatus", 0).apply()

        // return to start screen
        startActivity(Intent(applicationContext, MainActivity::class.java))
        finish()

    }

    override fun onResume() {
        super.onResume()
        updateListView()
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

            val reminderMessages = db.messageDao().getReminders()
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
}