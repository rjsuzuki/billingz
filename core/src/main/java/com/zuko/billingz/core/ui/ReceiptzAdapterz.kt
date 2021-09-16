/*
 * Copyright 2021 rjsuzuki
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.zuko.billingz.core.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.RecyclerView
import com.android.billingclient.api.PurchaseHistoryRecord
import com.zuko.billingz.R
import com.zuko.billingz.databinding.ListItemHistoryRecordBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * RecyclerView Adapter for a mutable list of [PurchaseHistoryRecord] objects.
 * @constructor
 * @param list
 */
class ReceiptzAdapterz(private val list: MutableList<PurchaseHistoryRecord>) : RecyclerView.Adapter<ReceiptzAdapterz.HistoryRecordViewHolder>() {

    inner class HistoryRecordViewHolder(val binding: ListItemHistoryRecordBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryRecordViewHolder {
        val binding = ListItemHistoryRecordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HistoryRecordViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryRecordViewHolder, position: Int) {
        val item = list[position]
        holder.binding.recordSku.text = item.toString()
        val time = SimpleDateFormat("MM-dd-yyyy hh:mm", Locale.getDefault()).format(Date(item.purchaseTime))
        holder.binding.recordTime.text = time
        // item.sku
        // item.signature
        // item.purchaseTime
        // item.purchaseToken
        // item.developerPayload
        // item.originalJson

        holder.binding.root.animation = AnimationUtils.loadAnimation(holder.binding.root.context, R.anim.product_item_anim)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    companion object {
        private const val TAG = "ReceiptsAdapter"
    }
}
