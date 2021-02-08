package com.example.mobicomphomework

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity

class MessageActivity : AppCompatActivity() {
    private lateinit var listView: ListView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message)

        listView = findViewById<ListView>(R.id.reminderMsgListView)

        findViewById<Button>(R.id.btnLogout).setOnClickListener { logOut() }

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
        // create placeholder list of reminder messages for listView
        val listItems = listOf(
                ReminderMessage(msgid = 1, name = "Event 1", time = "12:00 21.2.2021"),
                ReminderMessage(msgid = 2, name = "Event 2", time = "13:00 22.2.2021"),
                ReminderMessage(msgid = 3, name = "Event 3", time = "14:00 4.3.2021"))

        // initialize an adapter for listView and set the content
        val adapter = MessageAdapter(this, listItems)
        listView.adapter = adapter
    }
}