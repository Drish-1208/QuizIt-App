// File: app/src/main/java/com/example/myapplication/ProfileActivity.kt
package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var changePasswordButton: Button
    private lateinit var usernameTextView: TextView
    private lateinit var emailTextView: TextView
    private lateinit var logoutButton: Button
    private lateinit var backButton: Button // Changed from ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = Firebase.auth

        // Initialize Views
        changePasswordButton = findViewById(R.id.changePasswordButton)
        usernameTextView = findViewById(R.id.usernameTextView)
        emailTextView = findViewById(R.id.emailTextView)
        logoutButton = findViewById(R.id.logoutButton)
        backButton = findViewById(R.id.backButton) // Initialize the back button

        // Set User Email
        val currentUser = auth.currentUser
        if (currentUser != null) {
            emailTextView.text = currentUser.email
            usernameTextView.text = currentUser.displayName
        } else {
            // If no user is found for some reason, redirect to login
            redirectToLogin()
        }

        // Set Listeners
        setupClickListeners()
    }

    private fun setupClickListeners() {
        // Logout Button Listener
        logoutButton.setOnClickListener {
            logoutUser()
        }

        // Back Button Listener
        backButton.setOnClickListener {
            val intent =Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
        changePasswordButton.setOnClickListener {

            val intent = Intent(this, changePasswordActivity::class.java)
            startActivity(intent)
        }
    }

    private fun logoutUser() {
        auth.signOut()
        redirectToLogin()
    }

    private fun redirectToLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            // Clear the activity stack so the user cannot go back to the profile page
            // after logging out.
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish() // Close the profile activity as well
    }
}
