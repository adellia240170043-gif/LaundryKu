package com.laundryku.app.util

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.laundryku.app.R

fun Throwable.toAuthErrorRes(): Int = when (this) {
    is FirebaseNetworkException -> R.string.error_network
    is FirebaseAuthWeakPasswordException -> R.string.error_auth_weak_password
    is FirebaseAuthUserCollisionException -> R.string.error_auth_email_in_use
    is FirebaseAuthInvalidUserException -> R.string.error_auth_user_not_found
    is FirebaseAuthInvalidCredentialsException -> R.string.error_auth_invalid_credentials
    is FirebaseTooManyRequestsException -> R.string.error_too_many_requests
    else -> R.string.error_unknown
}