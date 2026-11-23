package com.laundryku.app.ui.orders

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.laundryku.app.R
import com.laundryku.app.data.model.Order
import com.laundryku.app.data.model.OrderStatus
import com.laundryku.app.data.model.ServiceType
import com.laundryku.app.databinding.ItemOrderBinding
import com.laundryku.app.util.formatDate
import com.laundryku.app.util.formatRupiah
import com.laundryku.app.util.formatWeight

class OrderAdapter(private val onItemClick: (Order) -> Unit) :
    ListAdapter<Order, OrderAdapter.OrderViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = ItemOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class OrderViewHolder(private val binding: ItemOrderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(order: Order) {
            val context = binding.root.context
            val service = ServiceType.fromKey(order.serviceType)
            val status = OrderStatus.fromKey(order.status)

            binding.tvCustomerName.text = order.customerName
            binding.tvDetail.text = context.getString(
                R.string.order_detail_format,
                context.getString(service.labelRes),
                formatWeight(order.weight)
            )
            binding.tvDate.text = formatDate(order.createdAt)
            binding.tvTotal.text = formatRupiah(order.totalPrice)
            binding.tvStatus.setText(status.labelRes)
            ViewCompat.setBackgroundTintList(
                binding.tvStatus,
                ColorStateList.valueOf(ContextCompat.getColor(context, statusColor(status)))
            )
            binding.root.setOnClickListener { onItemClick(order) }
        }
    }

    private fun statusColor(status: OrderStatus): Int = when (status) {
        OrderStatus.DITERIMA -> R.color.status_diterima
        OrderStatus.DICUCI -> R.color.status_dicuci
        OrderStatus.SELESAI -> R.color.status_selesai
        OrderStatus.DIAMBIL -> R.color.status_diambil
    }

    object DiffCallback : DiffUtil.ItemCallback<Order>() {
        override fun areItemsTheSame(oldItem: Order, newItem: Order) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Order, newItem: Order) = oldItem == newItem
    }
}