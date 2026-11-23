package com.laundryku.app.ui.orders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.laundryku.app.R
import com.laundryku.app.data.model.Order
import com.laundryku.app.data.model.OrderStatus
import com.laundryku.app.data.model.ServiceType
import com.laundryku.app.databinding.FragmentOrderFormBinding
import com.laundryku.app.util.UiState
import com.laundryku.app.util.formatRupiah
import com.laundryku.app.util.formatWeight
import com.laundryku.app.util.isOnline

class OrderFormFragment : Fragment() {

    private var _binding: FragmentOrderFormBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OrderViewModel by activityViewModels()

    private var orderId: String = ""
    private var existingOrder: Order? = null
    private var selectedService = ServiceType.CUCI_KERING
    private var selectedStatus = OrderStatus.DITERIMA
    private var isDeleting = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrderFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        orderId = arguments?.getString("orderId").orEmpty()
        val isEdit = orderId.isNotEmpty()
        existingOrder = if (isEdit) viewModel.findOrder(orderId) else null

        // Mode edit tapi datanya tidak ditemukan: kembali ke daftar
        if (isEdit && existingOrder == null) {
            findNavController().popBackStack()
            return
        }

        binding.tvFormTitle.setText(if (isEdit) R.string.title_edit_order else R.string.title_add_order)
        binding.btnDelete.isVisible = isEdit

        setupDropdowns()

        if (savedInstanceState != null) {
            // Layar diputar: pulihkan pilihan dropdown
            selectedService = ServiceType.fromKey(savedInstanceState.getString(KEY_SERVICE).orEmpty())
            selectedStatus = OrderStatus.fromKey(savedInstanceState.getString(KEY_STATUS).orEmpty())
            isDeleting = savedInstanceState.getBoolean(KEY_DELETING)
        } else {
            existingOrder?.let { fillForm(it) }
        }
        showSelections()

        binding.etWeight.doOnTextChanged { _, _, _, _ -> updateTotal() }
        binding.btnSave.setOnClickListener { save() }
        binding.btnDelete.setOnClickListener { confirmDelete() }
        binding.btnCancel.setOnClickListener { findNavController().popBackStack() }

        viewModel.saveState.observe(viewLifecycleOwner) { handleSaveState(it) }
    }

    private fun setupDropdowns() {
        val serviceLabels = ServiceType.values().map { getString(it.labelRes) }
        binding.actService.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, serviceLabels)
        )
        binding.actService.setOnItemClickListener { _, _, position, _ ->
            selectedService = ServiceType.values()[position]
            updateTotal()
        }

        val statusLabels = OrderStatus.values().map { getString(it.labelRes) }
        binding.actStatus.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, statusLabels)
        )
        binding.actStatus.setOnItemClickListener { _, _, position, _ ->
            selectedStatus = OrderStatus.values()[position]
        }
    }

    private fun fillForm(order: Order) {
        binding.etCustomerName.setText(order.customerName)
        binding.etWeight.setText(formatWeight(order.weight))
        selectedService = ServiceType.fromKey(order.serviceType)
        selectedStatus = OrderStatus.fromKey(order.status)
    }

    private fun showSelections() {
        binding.actService.setText(getString(selectedService.labelRes), false)
        binding.actStatus.setText(getString(selectedStatus.labelRes), false)
        updateTotal()
    }

    // "3,5" atau "3.5" -> 3.5
    private fun parseWeight(): Double? =
        binding.etWeight.text.toString().trim().replace(',', '.').toDoubleOrNull()

    private fun updateTotal() {
        val weight = parseWeight()
        val total = if (weight != null && weight > 0) selectedService.calculateTotal(weight) else 0L
        binding.tvTotalValue.text = formatRupiah(total)
    }

    private fun save() {
        val name = binding.etCustomerName.text.toString().trim()
        val weight = parseWeight()

        binding.tilCustomerName.error =
            if (name.isEmpty()) getString(R.string.error_customer_empty) else null
        binding.tilWeight.error = when {
            binding.etWeight.text.isNullOrBlank() -> getString(R.string.error_weight_empty)
            weight == null || weight <= 0.0 -> getString(R.string.error_weight_invalid)
            else -> null
        }

        if (binding.tilCustomerName.error != null ||
            binding.tilWeight.error != null ||
            weight == null
        ) return
        if (!ensureOnline()) return

        val order = Order(
            id = orderId,
            customerName = name,
            serviceType = selectedService.name,
            weight = weight,
            totalPrice = selectedService.calculateTotal(weight),
            status = selectedStatus.name,
            createdAt = existingOrder?.createdAt ?: System.currentTimeMillis(),
            photo = existingOrder?.photo.orEmpty()
        )
        isDeleting = false
        viewModel.saveOrder(order)
    }

    private fun confirmDelete() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_delete_title)
            .setMessage(R.string.dialog_delete_message)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                if (ensureOnline()) {
                    isDeleting = true
                    viewModel.deleteOrder(orderId)
                }
            }
            .show()
    }

    private fun ensureOnline(): Boolean {
        if (requireContext().isOnline()) return true
        Snackbar.make(binding.root, R.string.error_network, Snackbar.LENGTH_LONG).show()
        return false
    }

    private fun handleSaveState(state: UiState<Unit>) {
        when (state) {
            is UiState.Idle -> setLoading(false)
            is UiState.Loading -> setLoading(true)
            is UiState.Success -> {
                setLoading(false)
                val message = if (isDeleting) R.string.msg_order_deleted else R.string.msg_order_saved
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                viewModel.resetSaveState()
                findNavController().popBackStack()
            }
            is UiState.Error -> {
                setLoading(false)
                Snackbar.make(binding.root, state.messageRes, Snackbar.LENGTH_LONG).show()
                viewModel.resetSaveState()
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.isVisible = loading
        binding.btnSave.isEnabled = !loading
        binding.btnDelete.isEnabled = !loading
        binding.btnCancel.isEnabled = !loading
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_SERVICE, selectedService.name)
        outState.putString(KEY_STATUS, selectedStatus.name)
        outState.putBoolean(KEY_DELETING, isDeleting)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val KEY_SERVICE = "service"
        private const val KEY_STATUS = "status"
        private const val KEY_DELETING = "deleting"
    }
}