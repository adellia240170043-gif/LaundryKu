package com.laundryku.app.ui.orders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.laundryku.app.data.model.Order
import com.laundryku.app.data.model.OrderStatus
import com.laundryku.app.databinding.FragmentSummaryBinding
import com.laundryku.app.util.UiState
import com.laundryku.app.util.formatRupiah

class SummaryFragment : Fragment() {

    private var _binding: FragmentSummaryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OrderViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.ordersState.observe(viewLifecycleOwner) { state ->
            if (state is UiState.Success) showSummary(state.data)
        }
    }

    private fun showSummary(orders: List<Order>) {
        binding.tvTotalOrders.text = orders.size.toString()
        binding.tvTotalValue.text = formatRupiah(orders.sumOf { it.totalPrice })
        binding.tvCountDiterima.text = countOf(orders, OrderStatus.DITERIMA)
        binding.tvCountDicuci.text = countOf(orders, OrderStatus.DICUCI)
        binding.tvCountSelesai.text = countOf(orders, OrderStatus.SELESAI)
        binding.tvCountDiambil.text = countOf(orders, OrderStatus.DIAMBIL)
    }

    private fun countOf(orders: List<Order>, status: OrderStatus): String =
        orders.count { it.status == status.name }.toString()

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}