package com.example.mobicomphomework

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // save a placeholder username and password to sharedPreferences
        val sharedPref = applicationContext.getSharedPreferences(getString(R.string.sharedPrefKey),
                Context.MODE_PRIVATE) ?: return
        with (sharedPref.edit()) {
            putString("Username", "user")
            putString("Password", "password")
            apply()
        }

        findViewById<Button>(R.id.btnChooseLogin).setOnClickListener {
            startActivity(Intent(applicationContext, LoginActivity::class.java))
        }

        checkLoginStatus()
    }

    override fun onResume() {
        super.onResume()
        checkLoginStatus()
    }

    private fun checkLoginStatus() {
        val loginStatus = applicationContext.getSharedPreferences(getString(R.string.sharedPrefKey),
                Context.MODE_PRIVATE).getInt("LoginStatus", 0)
        if (loginStatus == 1) {
            startActivity(Intent(applicationContext, MessageActivity::class.java))
        }
    }

}