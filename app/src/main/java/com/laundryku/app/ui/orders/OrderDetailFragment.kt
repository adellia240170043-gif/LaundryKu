package com.laundryku.app.ui.orders

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.laundryku.app.R
import com.laundryku.app.data.model.Order
import com.laundryku.app.data.model.OrderStatus
import com.laundryku.app.data.model.ServiceType
import com.laundryku.app.databinding.FragmentOrderDetailBinding
import com.laundryku.app.util.UiState
import com.laundryku.app.util.formatDate
import com.laundryku.app.util.formatRupiah
import com.laundryku.app.util.formatWeight
import com.laundryku.app.util.isOnline

class OrderDetailFragment : Fragment() {

    private var _binding: FragmentOrderDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OrderViewModel by activityViewModels()

    private var orderId = ""
    private var isDeleting = false
    private var isLeaving = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrderDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        orderId = arguments?.getString("orderId").orEmpty()
        isDeleting = savedInstanceState?.getBoolean(KEY_DELETING) ?: false

        binding.btnEdit.setOnClickListener {
            findNavController().navigate(
                R.id.orderFormFragment,
                bundleOf("orderId" to orderId)
            )
        }
        binding.btnDelete.setOnClickListener { confirmDelete() }
        binding.btnBack.setOnClickListener { leave() }

        // Data selalu mengikuti perubahan terbaru dari Firestore
        viewModel.ordersState.observe(viewLifecycleOwner) { state ->
            if (state !is UiState.Success) return@observe
            val order = state.data.firstOrNull { it.id == orderId }
            if (order != null) {
                showOrder(order)
            } else if (!isDeleting) {
                leave()
            }
        }

        viewModel.saveState.observe(viewLifecycleOwner) { handleDeleteState(it) }
    }

    private fun showOrder(order: Order) {
        val service = ServiceType.fromKey(order.serviceType)
        val status = OrderStatus.fromKey(order.status)

        binding.tvCustomerName.text = order.customerName
        binding.tvService.setText(service.labelRes)
        binding.tvWeight.text = getString(R.string.weight_value_format, formatWeight(order.weight))
        binding.tvDate.text = formatDate(order.createdAt)
        binding.tvTotal.text = formatRupiah(order.totalPrice)
        binding.tvStatus.setText(status.labelRes)
        ViewCompat.setBackgroundTintList(
            binding.tvStatus,
            ColorStateList.valueOf(ContextCompat.getColor(requireContext(), statusColor(status)))
        )
    }

    private fun statusColor(status: OrderStatus): Int = when (status) {
        OrderStatus.DITERIMA -> R.color.status_diterima
        OrderStatus.DICUCI -> R.color.status_dicuci
        OrderStatus.SELESAI -> R.color.status_selesai
        OrderStatus.DIAMBIL -> R.color.status_diambil
    }

    private fun confirmDelete() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_delete_title)
            .setMessage(R.string.dialog_delete_message)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                if (requireContext().isOnline()) {
                    isDeleting = true
                    viewModel.deleteOrder(orderId)
                } else {
                    Snackbar.make(binding.root, R.string.error_network, Snackbar.LENGTH_LONG).show()
                }
            }
            .show()
    }

    private fun handleDeleteState(state: UiState<Unit>) {
        when (state) {
            is UiState.Idle -> setLoading(false)
            is UiState.Loading -> setLoading(true)
            is UiState.Success -> {
                setLoading(false)
                Toast.makeText(requireContext(), R.string.msg_order_deleted, Toast.LENGTH_SHORT).show()
                viewModel.resetSaveState()
                leave()
            }
            is UiState.Error -> {
                setLoading(false)
                isDeleting = false
                Snackbar.make(binding.root, state.messageRes, Snackbar.LENGTH_LONG).show()
                viewModel.resetSaveState()
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.isVisible = loading
        binding.btnEdit.isEnabled = !loading
        binding.btnDelete.isEnabled = !loading
        binding.btnBack.isEnabled = !loading
    }

    // Pastikan hanya kembali satu kali, supaya tidak keluar dari daftar sekaligus
    private fun leave() {
        if (isLeaving) return
        isLeaving = true
        findNavController().popBackStack()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_DELETING, isDeleting)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val KEY_DELETING = "deleting"
    }
}