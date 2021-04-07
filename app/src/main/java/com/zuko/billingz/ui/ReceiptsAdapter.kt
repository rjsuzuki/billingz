package com.zuko.billingz.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.RecyclerView
import com.android.billingclient.api.PurchaseHistoryRecord
import com.zuko.billingz.R
import com.zuko.billingz.databinding.ListItemHistoryRecordBinding

class ReceiptsAdapter(private val list: MutableList<PurchaseHistoryRecord>) : RecyclerView.Adapter<ReceiptsAdapter.HistoryRecordViewHolder>() {

    inner class HistoryRecordViewHolder(val binding: ListItemHistoryRecordBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryRecordViewHolder {
        val binding = ListItemHistoryRecordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HistoryRecordViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryRecordViewHolder, position: Int) {
        val item = list[position]
        item.sku
        item.signature
        item.purchaseTime
        item.purchaseToken
        item.developerPayload
        item.originalJson

        holder.binding.root.animation = AnimationUtils.loadAnimation(holder.binding.root.context, R.anim.product_item_anim)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    companion object {
        private const val TAG = "ReceiptsAdapter"
    }
}
