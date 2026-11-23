package com.laundryku.app.data.model

data class Order(
    val id: String = "",
    val customerName: String = "",
    val serviceType: String = ServiceType.CUCI_KERING.name,
    val weight: Double = 0.0,
    val totalPrice: Long = 0L,
    val status: String = OrderStatus.DITERIMA.name,
    val createdAt: Long = 0L,
    val photo: String = ""
) {
    fun toMap(): Map<String, Any> = mapOf(
        "customerName" to customerName,
        "serviceType" to serviceType,
        "weight" to weight,
        "totalPrice" to totalPrice,
        "status" to status,
        "createdAt" to createdAt,
        "photo" to photo
    )
}