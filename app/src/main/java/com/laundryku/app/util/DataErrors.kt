package com.laundryku.app.util

import com.google.firebase.firestore.FirebaseFirestoreException
import com.laundryku.app.R

fun Throwable.toDataErrorRes(): Int {
    if (this is FirebaseFirestoreException) {
        return when (code) {
            FirebaseFirestoreException.Code.UNAVAILABLE -> R.string.error_network
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> R.string.error_permission
            else -> R.string.error_unknown
        }
    }
    return R.string.error_unknown
}