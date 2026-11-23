package com.laundryku.app.data.model

import androidx.annotation.StringRes
import com.laundryku.app.R

enum class ServiceType(@StringRes val labelRes: Int, val pricePerKg: Long) {
    CUCI_KERING(R.string.service_cuci_kering, 6000L),
    CUCI_SETRIKA(R.string.service_cuci_setrika, 8000L),
    SETRIKA_SAJA(R.string.service_setrika_saja, 5000L);

    fun calculateTotal(weightKg: Double): Long = Math.round(weightKg * pricePerKg)

    companion object {
        fun fromKey(key: String): ServiceType =
            values().firstOrNull { it.name == key } ?: CUCI_KERING
    }
}