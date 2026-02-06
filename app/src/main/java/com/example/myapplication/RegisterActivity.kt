package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.ktx.userProfileChangeRequest // IMPORTANT: Import this
import com.google.firebase.ktx.Firebase

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = Firebase.auth

        // --- Find UI elements, INCLUDING the username field ---
        val usernameEditText = findViewById<TextInputEditText>(R.id.usernameEditText) // We need this again
        val emailEditText = findViewById<TextInputEditText>(R.id.emailEditText)
        val passwordEditText = findViewById<TextInputEditText>(R.id.passwordEditText)
        val registerButton = findViewById<TextView>(R.id.registerButton)
        val loginTextView = findViewById<TextView>(R.id.loginTextView)

        loginTextView.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        registerButton.setOnClickListener {
            // --- Get all three inputs again ---
            val username = usernameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            // --- Validate all three inputs ---
            if (username.isBlank() || email.isBlank() || password.isBlank()) {
                Toast.makeText(this, "Please fill out all fields.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Log.d("RegisterActivity", "createUserWithEmail:success")
                        val firebaseUser = auth.currentUser!! // Get the newly created user

                        // --- THIS IS THE NEW PART ---
                        // 1. Create a profile update request to set the username
                        val profileUpdates = userProfileChangeRequest {
                            displayName = username
                        }

                        // 2. Update the user's profile
                        firebaseUser.updateProfile(profileUpdates)
                            .addOnCompleteListener { profileTask ->
                                if (profileTask.isSuccessful) {
                                    Log.d("RegisterActivity", "User profile updated with username.")
                                }
                                // This part runs whether the profile update succeeds or fails.
                                // We can navigate the user immediately.
                                Toast.makeText(this, "Registration Successful!", Toast.LENGTH_SHORT).show()
                                navigateToMain()
                            }
                        // --- END OF NEW PART ---

                    } else {
                        // Authentication failed
                        Log.w("RegisterActivity", "createUserWithEmail:failure", task.exception)
                        Toast.makeText(this, "Authentication failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }

    // Helper function to avoid repeating code
    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
