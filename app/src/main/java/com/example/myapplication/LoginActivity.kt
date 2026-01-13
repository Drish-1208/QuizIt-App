package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val emailEditText = findViewById<TextInputEditText>(R.id.emailEditText)
        val passwordEditText = findViewById<TextInputEditText>(R.id.passwordEditText)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val registerTextView = findViewById<TextView>(R.id.registerTextView)

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            val savedEmail = sharedPreferences.getString("email", null)
            val savedPassword = sharedPreferences.getString("password", null)

            if (email == savedEmail && password == savedPassword && savedEmail != null) {
                sharedPreferences.edit().putBoolean("isLoggedIn", true).apply()

                Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show()

                // takes to main scren
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)

                finish() // Finis login
            } else {
                Toast.makeText(this, "Invalid email or password.", Toast.LENGTH_SHORT).show()
            }
        }
        registerTextView.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }
}

