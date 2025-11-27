package com.hcmus.forumus_client.ui.auth.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.hcmus.forumus_client.data.model.User
import com.hcmus.forumus_client.data.repository.AuthRepository
import com.hcmus.forumus_client.utils.Resource
import com.hcmus.forumus_client.utils.ValidationUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository(
        FirebaseAuth.getInstance(), 
        FirebaseFirestore.getInstance(),
        context = application.applicationContext
    )
    
    private val _loginState = MutableLiveData<Resource<User>>()
    val loginState: LiveData<Resource<User>> = _loginState

    fun login(email: String, password: String, rememberMe: Boolean = false) {
        if (!ValidationUtils.isValidEmail(email)) {
            _loginState.value = Resource.Error("Please enter a valid email")
            return
        }

        if (password.isBlank()) {
            _loginState.value = Resource.Error("Please enter your password")
            return
        }

        _loginState.value = Resource.Loading()

        viewModelScope.launch {
            val result = authRepository.login(email, password, rememberMe)
            _loginState.value = result
        }
    }
}