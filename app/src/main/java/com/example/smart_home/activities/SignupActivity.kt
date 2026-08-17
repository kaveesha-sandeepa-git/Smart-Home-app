package com.example.smart_home.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.smart_home.R
import com.example.smart_home.repository.SmartHomeRepository
import com.example.smart_home.utils.PreferencesManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest

class SignupActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var progressBar: ProgressBar
    private lateinit var preferencesManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        auth = FirebaseAuth.getInstance()
        preferencesManager = PreferencesManager.getInstance(this)

        val usernameEditText = findViewById<TextInputEditText>(R.id.et_username)
        val emailEditText = findViewById<TextInputEditText>(R.id.et_email)
        val passwordEditText = findViewById<TextInputEditText>(R.id.et_password)
        val createAccountButton = findViewById<MaterialButton>(R.id.btn_create_account)
        val loginLink = findViewById<TextView>(R.id.btn_login_link)
        progressBar = findViewById(R.id.progress_bar)

        createAccountButton.setOnClickListener {
            val username = usernameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (username.isEmpty()) {
                Toast.makeText(this, "Please enter a username", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            showLoading(true)
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val newUserId = auth.currentUser?.uid ?: ""
                        if (preferencesManager.userId != newUserId) {
                            preferencesManager.userId = newUserId
                            SmartHomeRepository.getInstance(this).clearData()
                        }
                        // Save display name
                        val profileUpdate = UserProfileChangeRequest.Builder()
                            .setDisplayName(username)
                            .build()
                        auth.currentUser?.updateProfile(profileUpdate)
                            ?.addOnCompleteListener {
                                showLoading(false)
                                Toast.makeText(this, "Account created!", Toast.LENGTH_SHORT).show()
                                val intent = Intent(this, MainActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                                finish()
                            }
                    } else {
                        showLoading(false)
                        Toast.makeText(
                            this,
                            "Registration failed: ${task.exception?.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }

        loginLink.setOnClickListener {
            finish() // go back to Login
        }
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
}
