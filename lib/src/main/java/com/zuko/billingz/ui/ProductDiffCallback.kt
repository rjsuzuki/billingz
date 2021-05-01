package com.zuko.billingz.ui

import androidx.recyclerview.widget.DiffUtil
import com.zuko.billingz.lib.store.products.Product

class ProductDiffCallback(private val oldList: MutableList<Product>, private val newList: MutableList<Product>) : DiffUtil.Callback() {

    override fun getOldListSize(): Int {
        return oldList.size
    }

    override fun getNewListSize(): Int {
        return newList.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].id == newList[newItemPosition].id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].description == newList[newItemPosition].description &&
            oldList[oldItemPosition].details == newList[newItemPosition].details &&
            oldList[oldItemPosition].name == newList[newItemPosition].name &&
            oldList[oldItemPosition].price == newList[newItemPosition].price &&
            oldList[oldItemPosition].sku == newList[newItemPosition].sku &&
            oldList[oldItemPosition].skuType == newList[newItemPosition].skuType &&
            oldList[oldItemPosition].type == newList[newItemPosition].type
    }
}
