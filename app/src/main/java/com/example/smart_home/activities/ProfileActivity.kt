package com.example.smart_home.activities

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.smart_home.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth

class ProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Profile"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        val user = FirebaseAuth.getInstance().currentUser
        val displayName = user?.displayName ?: user?.email?.substringBefore("@") ?: "User"
        val email = user?.email ?: ""
        val initials = displayName.take(1).uppercase()

        findViewById<TextView>(R.id.tv_avatar_initials).text = initials
        findViewById<TextView>(R.id.tv_user_name).text = displayName
        findViewById<TextView>(R.id.tv_user_email).text = email

        findViewById<MaterialButton>(R.id.btn_logout).setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        findViewById<android.view.View>(R.id.row_edit_profile).setOnClickListener {
            Toast.makeText(this, "Edit Profile (coming soon)", Toast.LENGTH_SHORT).show()
        }

        findViewById<android.view.View>(R.id.row_change_password).setOnClickListener {
            Toast.makeText(this, "Change Password (coming soon)", Toast.LENGTH_SHORT).show()
        }
    }
}
