package com.laundryku.app.ui.orders

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.laundryku.app.R
import com.laundryku.app.data.model.Order
import com.laundryku.app.data.model.OrderStatus
import com.laundryku.app.databinding.FragmentOrderListBinding
import com.laundryku.app.ui.auth.AuthViewModel
import com.laundryku.app.ui.auth.LoginActivity
import com.laundryku.app.util.UiState

class OrderListFragment : Fragment() {

    private var _binding: FragmentOrderListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OrderViewModel by activityViewModels()
    private val authViewModel: AuthViewModel by activityViewModels()
    private lateinit var adapter: OrderAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrderListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Ketuk kartu: buka halaman detail
        adapter = OrderAdapter { order ->
            findNavController().navigate(
                R.id.orderDetailFragment,
                bundleOf("orderId" to order.id)
            )
        }
        binding.rvOrders.adapter = adapter

        binding.etSearch.doOnTextChanged { text, _, _, _ ->
            viewModel.setQuery(text?.toString().orEmpty())
        }
        binding.chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            viewModel.setStatusFilter(statusFromChip(checkedIds.firstOrNull()))
        }
        binding.btnRetry.setOnClickListener { viewModel.retry() }
        binding.btnLogout.setOnClickListener { logout() }

        // Ketuk tombol +: buka form dalam mode tambah
        binding.fabAdd.setOnClickListener {
            findNavController().navigate(R.id.orderFormFragment)
        }

        viewModel.visibleOrders.observe(viewLifecycleOwner) { render(it) }
    }

    private fun statusFromChip(chipId: Int?): String? = when (chipId) {
        R.id.chipDiterima -> OrderStatus.DITERIMA.name
        R.id.chipDicuci -> OrderStatus.DICUCI.name
        R.id.chipSelesai -> OrderStatus.SELESAI.name
        R.id.chipDiambil -> OrderStatus.DIAMBIL.name
        else -> null
    }

    private fun render(state: UiState<List<Order>>) {
        binding.progress.isVisible = state is UiState.Loading
        binding.layoutError.isVisible = state is UiState.Error
        binding.rvOrders.isVisible = false
        binding.tvEmpty.isVisible = false

        when (state) {
            is UiState.Success -> {
                adapter.submitList(state.data)
                val empty = state.data.isEmpty()
                binding.rvOrders.isVisible = !empty
                binding.tvEmpty.isVisible = empty
                binding.tvEmpty.setText(
                    if (viewModel.hasAnyOrders()) R.string.empty_search else R.string.empty_orders
                )
            }
            is UiState.Error -> binding.tvError.setText(state.messageRes)
            else -> Unit
        }
    }

    private fun logout() {
        authViewModel.logout()
        val intent = Intent(requireContext(), LoginActivity::class.java).addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        )
        startActivity(intent)
    }

    override fun onDestroyView() {
        binding.rvOrders.adapter = null
        _binding = null
        super.onDestroyView()
    }
}