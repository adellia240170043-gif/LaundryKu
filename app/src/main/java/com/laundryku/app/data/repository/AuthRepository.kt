package com.laundryku.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest

class AuthRepository {

    private val auth = FirebaseAuth.getInstance()

    val currentUser: FirebaseUser? get() = auth.currentUser

    fun login(email: String, password: String, onResult: (Result<Unit>) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    fun register(
        name: String,
        email: String,
        password: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val profile = UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()
                val user = authResult.user
                if (user == null) {
                    onResult(Result.success(Unit))
                } else {
                    user.updateProfile(profile)
                        .addOnCompleteListener { onResult(Result.success(Unit)) }
                }
            }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    fun logout() = auth.signOut()
}