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

        // Find all the input fields
        val usernameEditText = findViewById<TextInputEditText>(R.id.usernameEditText) // New
        val emailEditText = findViewById<TextInputEditText>(R.id.emailEditText)
        val passwordEditText = findViewById<TextInputEditText>(R.id.passwordEditText)
        val registerButton = findViewById<Button>(R.id.registerButton)

        registerButton.setOnClickListener {
            // Get text from all fields
            val username = usernameEditText.text.toString() // New
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            // Make sure no fields are blank
            if (username.isNotBlank() && email.isNotBlank() && password.isNotBlank()) {
                // --- Save All User Data ---
                val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                with(sharedPreferences.edit()) {
                    putString("username", username) // New: Save the username
                    putString("email", email)
                    putString("password", password)
                    apply() // Save the changes
                }

                Toast.makeText(this, "Registration Successful!", Toast.LENGTH_SHORT).show()

                // Go back to the login screen
                finish()
            } else {
                Toast.makeText(this, "Please fill out all fields.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
