package com.laundryku.app.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val localeId = Locale("id", "ID")

// 28000 -> Rp28.000
fun formatRupiah(amount: Long): String =
    "Rp" + String.format(localeId, "%,d", amount)

// 3.5 -> 3,5   dan   3.0 -> 3
fun formatWeight(weight: Double): String =
    String.format(localeId, "%.2f", weight).trimEnd('0').trimEnd(',')

// 1763800000000 -> 22 Nov 2026
fun formatDate(millis: Long): String =
    SimpleDateFormat("d MMM yyyy", localeId).format(Date(millis))