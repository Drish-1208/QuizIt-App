package com.example.myapplication // Make sure this matches your app's package name

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class changePasswordActivity : AppCompatActivity() {

    // --- Firebase Auth ---
    private val auth = Firebase.auth

    // --- Views ---
    private lateinit var newPasswordEditText: TextInputEditText
    private lateinit var confirmPasswordEditText: TextInputEditText
    private lateinit var updatePasswordButton: Button
    private lateinit var backButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)

        // Initialize views using their IDs from the XML
        newPasswordEditText = findViewById(R.id.newPasswordEditText)
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText)
        updatePasswordButton = findViewById(R.id.updatePasswordButton)
        backButton = findViewById(R.id.backButton)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        // This is the button that's causing the problem. We will fix its logic.
        updatePasswordButton.setOnClickListener {
            updateUserPassword()
        }

        // This button correctly takes the user back.
        backButton.setOnClickListener {
            // Finishes this activity and returns to the previous one (ProfileActivity)
            finish()
        }
    }

    private fun updateUserPassword() {
        val newPassword = newPasswordEditText.text.toString().trim()
        val confirmPassword = confirmPasswordEditText.text.toString().trim()

        // --- Validation Step ---
        if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please fill in both password fields.", Toast.LENGTH_SHORT).show()
            return
        }

        if (newPassword != confirmPassword) {
            Toast.makeText(this, "Passwords do not match.", Toast.LENGTH_SHORT).show()
            return
        }

        if (newPassword.length < 6) {
            Toast.makeText(this, "Password must be at least 6 characters long.", Toast.LENGTH_SHORT).show()
            return
        }

        // --- Firebase Update Step ---
        val user = auth.currentUser
        if (user == null) {
            // This should not happen if the user is coming from the profile screen, but it's good to check.
            Toast.makeText(this, "No user is logged in. Returning to login screen.", Toast.LENGTH_LONG).show()
            val intent = Intent(this, LoginActivity::class.java) // Redirect to Login
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            return
        }

        // Now, update the password in Firebase
        user.updatePassword(newPassword)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Success!
                    Toast.makeText(this, "Password updated successfully!", Toast.LENGTH_SHORT).show()
                    // Go back to the profile screen
                    finish()
                } else {
                    // Failure. Show an error message.
                    val errorMessage = task.exception?.message ?: "Failed to update password. Please try again."
                    Toast.makeText(this, "Error: $errorMessage", Toast.LENGTH_LONG).show()
                }
            }
    }
}
