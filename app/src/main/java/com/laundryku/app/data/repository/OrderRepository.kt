package com.laundryku.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.laundryku.app.data.model.Order

class OrderRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Lokasi data: users/{uid}/orders
    private fun ordersCollection(): CollectionReference? {
        val uid = auth.currentUser?.uid ?: return null
        return db.collection("users").document(uid).collection("orders")
    }

    // Read: data diperbarui otomatis setiap ada perubahan
    fun listenOrders(onResult: (Result<List<Order>>) -> Unit): ListenerRegistration? {
        val collection = ordersCollection() ?: return null
        return collection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onResult(Result.failure(error))
                    return@addSnapshotListener
                }
                val orders = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Order::class.java)?.copy(id = doc.id)
                }.orEmpty()
                onResult(Result.success(orders))
            }
    }

    // Create
    fun addOrder(order: Order, onResult: (Result<Unit>) -> Unit) {
        val collection = ordersCollection()
            ?: return onResult(Result.failure(IllegalStateException("Belum login")))
        collection.add(order.toMap())
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    // Update
    fun updateOrder(order: Order, onResult: (Result<Unit>) -> Unit) {
        val collection = ordersCollection()
            ?: return onResult(Result.failure(IllegalStateException("Belum login")))
        collection.document(order.id).set(order.toMap())
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    // Delete
    fun deleteOrder(orderId: String, onResult: (Result<Unit>) -> Unit) {
        val collection = ordersCollection()
            ?: return onResult(Result.failure(IllegalStateException("Belum login")))
        collection.document(orderId).delete()
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }
}