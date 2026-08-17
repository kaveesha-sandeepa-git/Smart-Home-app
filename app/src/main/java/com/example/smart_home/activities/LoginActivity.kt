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

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var progressBar: ProgressBar
    private lateinit var preferencesManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        preferencesManager = PreferencesManager.getInstance(this)

        if (auth.currentUser != null) {
            val oldUserId = preferencesManager.userId
            val newUserId = auth.currentUser?.uid ?: ""
            if (oldUserId != newUserId) {
                preferencesManager.userId = newUserId
                SmartHomeRepository.getInstance(this).clearData()
            }
            navigateToMain()
            return
        }

        setContentView(R.layout.activity_login)

        val emailEditText = findViewById<TextInputEditText>(R.id.et_email)
        val passwordEditText = findViewById<TextInputEditText>(R.id.et_password)
        val loginButton = findViewById<MaterialButton>(R.id.btn_login)
        val registerLink = findViewById<TextView>(R.id.btn_register)
        progressBar = findViewById(R.id.progress_bar)

        loginButton.setOnClickListener {
// ...
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                showLoading(true)
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        showLoading(false)
                        if (task.isSuccessful) {
                            val newUserId = auth.currentUser?.uid ?: ""
                            if (preferencesManager.userId != newUserId) {
                                preferencesManager.userId = newUserId
                                SmartHomeRepository.getInstance(this).clearData()
                            }
                            navigateToMain()
                        } else {
                            Toast.makeText(
                                this,
                                "Authentication failed: ${task.exception?.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Please enter your email and password", Toast.LENGTH_SHORT).show()
            }
        }

        // Navigate to SignupActivity when Sign up link is tapped
        registerLink.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
