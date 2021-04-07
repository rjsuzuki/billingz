package com.zuko.billingz.ui

import com.zuko.billingz.lib.store.products.Product

interface ProductSelectedListener {

    fun onPurchaseRequested(product: Product)
    fun onEditRequested(product: Product)
    fun onProductDeleted(product: Product)
}
