package com.example.myapplication

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val usernameEditText = findViewById<TextInputEditText>(R.id.usernameEditText)
        val emailEditText = findViewById<TextInputEditText>(R.id.emailEditText)
        val passwordEditText = findViewById<TextInputEditText>(R.id.passwordEditText)
        val registerButton = findViewById<Button>(R.id.registerButton)

        registerButton.setOnClickListener {
            val username = usernameEditText.text.toString()
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            if (username.isNotBlank() && email.isNotBlank() && password.isNotBlank()) {
                val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                with(sharedPreferences.edit()) {
                    putString("username", username)
                    putString("email", email)
                    putString("password", password)
                    apply()
                }

                Toast.makeText(this, "Registration Successful!", Toast.LENGTH_SHORT).show()

                finish()
            } else {
                Toast.makeText(this, "Please fill out all fields.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
