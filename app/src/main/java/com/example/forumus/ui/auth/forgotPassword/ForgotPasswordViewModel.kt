package com.example.forumus.ui.auth.forgotPassword

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.forumus.data.repository.AuthRepository
import com.example.forumus.utils.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class ForgotPasswordViewModel : ViewModel() {
    private val authRepository = AuthRepository(
        FirebaseAuth.getInstance(),
        FirebaseFirestore.getInstance()
    )

    private val _accountExistsState = MutableLiveData<Resource<Boolean>>()
    val accountExistsState: LiveData<Resource<Boolean>> = _accountExistsState
    
    fun checkAccountExists(email: String) {
        _accountExistsState.value = Resource.Loading()

        viewModelScope.launch {
            val result = authRepository.checkAccountExists(email)
            _accountExistsState.value = result
        }
    }
}