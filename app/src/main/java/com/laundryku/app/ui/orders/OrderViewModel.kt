package com.laundryku.app.ui.orders

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ListenerRegistration
import com.laundryku.app.R
import com.laundryku.app.data.model.Order
import com.laundryku.app.data.repository.OrderRepository
import com.laundryku.app.util.UiState
import com.laundryku.app.util.toDataErrorRes

class OrderViewModel : ViewModel() {

    private val repository = OrderRepository()
    private var registration: ListenerRegistration? = null

    // Semua pesanan dari Firestore
    private var currentOrders: List<Order> = emptyList()
    private val allOrders = MutableLiveData<UiState<List<Order>>>(UiState.Loading)

    // Dipakai halaman Ringkasan (semua pesanan tanpa filter)
    val ordersState: LiveData<UiState<List<Order>>> get() = allOrders

    // Kata kunci pencarian dan filter status (null = semua status)
    private val query = MutableLiveData("")
    private val statusFilter = MutableLiveData<String?>(null)

    // Daftar yang tampil di layar (sudah dicari dan difilter)
    private val _visibleOrders = MediatorLiveData<UiState<List<Order>>>()
    val visibleOrders: LiveData<UiState<List<Order>>> = _visibleOrders

    // Status proses simpan atau hapus
    private val _saveState = MutableLiveData<UiState<Unit>>(UiState.Idle)
    val saveState: LiveData<UiState<Unit>> = _saveState

    init {
        _visibleOrders.addSource(allOrders) { applyFilter() }
        _visibleOrders.addSource(query) { applyFilter() }
        _visibleOrders.addSource(statusFilter) { applyFilter() }
        startListening()
    }

    private fun startListening() {
        allOrders.value = UiState.Loading
        registration?.remove()
        registration = repository.listenOrders { result ->
            result.onSuccess { currentOrders = it }
            allOrders.value = result.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.toDataErrorRes()) }
            )
        }
        if (registration == null) {
            allOrders.value = UiState.Error(R.string.error_unknown)
        }
    }

    fun retry() = startListening()

    private fun applyFilter() {
        val state = allOrders.value ?: return
        if (state !is UiState.Success) {
            _visibleOrders.value = state
            return
        }
        val keyword = query.value.orEmpty().trim()
        val status = statusFilter.value
        val filtered = currentOrders.filter { order ->
            (status == null || order.status == status) &&
                    (keyword.isEmpty() || order.customerName.contains(keyword, ignoreCase = true))
        }
        _visibleOrders.value = UiState.Success(filtered)
    }

    fun setQuery(text: String) {
        query.value = text
    }

    fun setStatusFilter(status: String?) {
        statusFilter.value = status
    }

    // True kalau database punya pesanan (dipakai untuk membedakan "kosong" dan "tidak ditemukan")
    fun hasAnyOrders(): Boolean = currentOrders.isNotEmpty()

    fun findOrder(id: String): Order? = currentOrders.firstOrNull { it.id == id }

    fun saveOrder(order: Order) {
        _saveState.value = UiState.Loading
        val callback: (Result<Unit>) -> Unit = { result -> publishSave(result) }
        if (order.id.isEmpty()) {
            repository.addOrder(order, callback)
        } else {
            repository.updateOrder(order, callback)
        }
    }

    fun deleteOrder(orderId: String) {
        _saveState.value = UiState.Loading
        repository.deleteOrder(orderId) { result -> publishSave(result) }
    }

    fun resetSaveState() {
        _saveState.value = UiState.Idle
    }

    private fun publishSave(result: Result<Unit>) {
        _saveState.value = result.fold(
            onSuccess = { UiState.Success(Unit) },
            onFailure = { UiState.Error(it.toDataErrorRes()) }
        )
    }

    override fun onCleared() {
        registration?.remove()
    }
}