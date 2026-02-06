package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class RegisterActivity : AppCompatActivity() {

    // 1. Declare Firebase variables
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // 2. Initialize Firebase Auth and Realtime Database
        auth = Firebase.auth
        database = Firebase.database.reference

        // Find your UI elements
        val usernameEditText = findViewById<TextInputEditText>(R.id.usernameEditText)
        val emailEditText = findViewById<TextInputEditText>(R.id.emailEditText)
        val passwordEditText = findViewById<TextInputEditText>(R.id.passwordEditText)
        val registerButton = findViewById<TextView>(R.id.registerButton)
        val loginTextView = findViewById<TextView>(R.id.loginTextView)

        loginTextView.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
        // Set the button's click listener
        registerButton.setOnClickListener {
            val username = usernameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            // Check that fields are not empty
            if (username.isBlank() || email.isBlank() || password.isBlank()) {
                Toast.makeText(this, "Please fill out all fields.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // --- 3. CREATE USER IN FIREBASE AUTHENTICATION ---
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Authentication successful
                        Log.d("RegisterActivity", "createUserWithEmail:success")
                        val firebaseUser = auth.currentUser

                        // --- THIS IS THE NEW PART ---
                        // Create a profile change request to set the username (displayName)
                        val profileUpdates = userProfileChangeRequest {
                            displayName = username
                        }

                        // Update the user's profile on Firebase Auth
                        firebaseUser!!.updateProfile(profileUpdates)
                            .addOnCompleteListener { profileTask ->
                                if (profileTask.isSuccessful) {
                                    Log.d("RegisterActivity", "User profile updated with username.")
                                }
                                // Proceed to save to database regardless of profile update success
                                // as it's not critical for login.
                            }

                        // --- 4. SAVE USER'S NAME AND EMAIL TO REALTIME DATABASE ---
                        val userId = firebaseUser.uid
                        saveUserToDatabase(userId, username, email)

                    } else{
                        // Authentication failed
                        Log.w("RegisterActivity", "createUserWithEmail:failure", task.exception)
                        Toast.makeText(this, "Authentication Successful.", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun saveUserToDatabase(userId: String, username: String, email: String) {
        // Create a User object (Assuming you have a User data class)
        val user = User(username, email)
        // Save the user object to the "users" node in the database, using the user's ID as the key
        database.child("users").child(userId).setValue(user)
            .addOnSuccessListener {
                Log.d("RegisterActivity", "User data saved successfully")
                Toast.makeText(this, "Registration Successful!", Toast.LENGTH_SHORT).show()

                // Navigate to the main activity after successful registration
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish() // Close the register activity
            }
            .addOnFailureListener { e ->
                Log.e("RegisterActivity", "Failed to save user data", e)
                Toast.makeText(this, "Failed to save data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // You should have a data class like this, preferably in its own file
    data class User(val username: String = "", val email: String = "")
}
