package com.laundryku.app.data.model

import androidx.annotation.StringRes
import com.laundryku.app.R

enum class OrderStatus(@StringRes val labelRes: Int) {
    DITERIMA(R.string.status_diterima),
    DICUCI(R.string.status_dicuci),
    SELESAI(R.string.status_selesai),
    DIAMBIL(R.string.status_diambil);

    companion object {
        fun fromKey(key: String): OrderStatus =
            values().firstOrNull { it.name == key } ?: DITERIMA
    }
}