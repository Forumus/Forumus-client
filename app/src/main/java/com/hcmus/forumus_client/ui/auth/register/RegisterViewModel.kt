package com.hcmus.forumus_client.ui.auth.register

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hcmus.forumus_client.data.model.User
import com.hcmus.forumus_client.data.model.UserRole
import com.hcmus.forumus_client.data.repository.AuthRepository
import com.hcmus.forumus_client.utils.Resource
import com.hcmus.forumus_client.utils.ValidationUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class RegisterViewModel : ViewModel() {

    private val authRepository = AuthRepository(FirebaseAuth.getInstance(), FirebaseFirestore.getInstance())
    
    private val _registerState = MutableLiveData<Resource<User>>()
    val registerState: LiveData<Resource<User>> = _registerState

    fun register(
        fullName: String,
        email: String,
        password: String,
        confirmPassword: String,
        role: UserRole
    ) {
        // Validation
        if (!ValidationUtils.isValidFullName(fullName)) {
            _registerState.value = Resource.Error("Please enter a valid full name")
            return
        }

        if (!ValidationUtils.isValidEmail(email)) {
            _registerState.value = Resource.Error("Please enter a valid email")
            return
        }

        if (!ValidationUtils.isValidStudentEmail(email)) {
            _registerState.value = Resource.Error("Please use your university email")
            return
        }

        if (!ValidationUtils.isValidPassword(password)) {
            _registerState.value = Resource.Error("Password must be at least 6 characters")
            return
        }

        if (password != confirmPassword) {
            _registerState.value = Resource.Error("Passwords do not match")
            return
        }

        _registerState.value = Resource.Loading()

        viewModelScope.launch {
            val result = authRepository.register(email, password, fullName, role)
            _registerState.value = result
        }
    }
}