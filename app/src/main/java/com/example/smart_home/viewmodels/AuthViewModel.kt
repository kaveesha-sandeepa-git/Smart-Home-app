package com.example.smart_home.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.example.smart_home.repository.AuthRepository
import com.google.firebase.auth.FirebaseUser

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AuthRepository()
    val userLiveData: LiveData<FirebaseUser?> = repository.userLiveData
    val authError: LiveData<String?> = repository.authError

    fun login(email: String, password: String) {
        repository.login(email, password)
    }

    fun register(email: String, password: String) {
        repository.register(email, password)
    }

    fun logout() {
        repository.logout()
    }

    fun clearError() {
        repository.clearError()
    }
}
