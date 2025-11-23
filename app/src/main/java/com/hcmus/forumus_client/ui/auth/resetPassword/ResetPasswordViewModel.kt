package com.hcmus.forumus_client.ui.auth.resetPassword

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hcmus.forumus_client.data.repository.AuthRepository
import com.hcmus.forumus_client.utils.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class ResetPasswordViewModel : ViewModel() {
    private val authRepository = AuthRepository(
        FirebaseAuth.getInstance(),
        FirebaseFirestore.getInstance()
    )

    private val _resetPasswordState = MutableLiveData<Resource<Boolean>>()
    val resetPasswordState: LiveData<Resource<Boolean>> = _resetPasswordState

    fun resetPassword(email: String, newPassword: String) {
        _resetPasswordState.value = Resource.Loading()

        viewModelScope.launch {
            val result = authRepository.resetPassword(email, newPassword)
            _resetPasswordState.value = result
        }
    }
}