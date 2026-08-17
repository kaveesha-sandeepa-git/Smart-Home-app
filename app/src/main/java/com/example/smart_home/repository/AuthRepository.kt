package com.example.smart_home.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class AuthRepository {

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val _userLiveData = MutableLiveData<FirebaseUser?>()
    val userLiveData: LiveData<FirebaseUser?> = _userLiveData

    private val _authError = MutableLiveData<String?>()
    val authError: LiveData<String?> = _authError

    init {
        _userLiveData.value = firebaseAuth.currentUser
    }

    fun login(email: String, password: String) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _userLiveData.value = firebaseAuth.currentUser
                } else {
                    _authError.value = task.exception?.message ?: "Login failed"
                }
            }
    }

    fun register(email: String, password: String) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _userLiveData.value = firebaseAuth.currentUser
                } else {
                    _authError.value = task.exception?.message ?: "Registration failed"
                }
            }
    }

    fun logout() {
        firebaseAuth.signOut()
        _userLiveData.value = null
    }

    fun clearError() {
        _authError.value = null
    }
}
