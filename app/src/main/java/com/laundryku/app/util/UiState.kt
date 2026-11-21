package com.laundryku.app.util

import androidx.annotation.StringRes

sealed class UiState<out T> {
    object Idle : UiState<Nothing>()
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(@StringRes val messageRes: Int) : UiState<Nothing>()
}