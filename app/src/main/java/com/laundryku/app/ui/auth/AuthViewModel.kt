package com.laundryku.app.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.laundryku.app.data.repository.AuthRepository
import com.laundryku.app.util.UiState
import com.laundryku.app.util.toAuthErrorRes

class AuthViewModel : ViewModel() {

    private val repository = AuthRepository()

    private val _authState = MutableLiveData<UiState<Unit>>(UiState.Idle)
    val authState: LiveData<UiState<Unit>> = _authState

    fun isLoggedIn(): Boolean = repository.currentUser != null

    fun currentUserLabel(): String? {
        val user = repository.currentUser ?: return null
        return user.displayName?.takeIf { it.isNotBlank() } ?: user.email
    }

    fun login(email: String, password: String) {
        _authState.value = UiState.Loading
        repository.login(email, password) { result -> publish(result) }
    }

    fun register(name: String, email: String, password: String) {
        _authState.value = UiState.Loading
        repository.register(name, email, password) { result -> publish(result) }
    }

    fun logout() = repository.logout()
    fun currentName(): String = repository.currentUser?.displayName.orEmpty()

    fun currentEmail(): String = repository.currentUser?.email.orEmpty()

    fun resetState() {
        _authState.value = UiState.Idle
    }

    private fun publish(result: Result<Unit>) {
        _authState.value = result.fold(
            onSuccess = { UiState.Success(Unit) },
            onFailure = { UiState.Error(it.toAuthErrorRes()) }
        )
    }
}