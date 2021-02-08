package com.example.mobicomphomework

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        findViewById<Button>(R.id.btnLogin).setOnClickListener { checkCredentials() }
    }

    private fun checkCredentials() {
        val sharedPref = applicationContext.getSharedPreferences(getString(R.string.sharedPrefKey), Context.MODE_PRIVATE) ?: return
        val username = sharedPref.getString("Username", "")
        val password = sharedPref.getString("Password", "")

        val usernameEntry = findViewById<EditText>(R.id.editTextUsername).text.toString()
        val passwordEntry = findViewById<EditText>(R.id.editTextPassword).text.toString()

        if ((username != "") && (password != "")) {
            if ((usernameEntry == username) && (passwordEntry == password)) {
                // store the login status
                sharedPref.edit().putInt("LoginStatus", 1).apply()

                // start the message activity
                startActivity(Intent(applicationContext, MessageActivity::class.java))
                finish()

            } else {
                Toast.makeText(
                    applicationContext,
                    "Wrong username or password",
                    Toast.LENGTH_SHORT).show()
            }
        }
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
            finish()
        }
    }
}