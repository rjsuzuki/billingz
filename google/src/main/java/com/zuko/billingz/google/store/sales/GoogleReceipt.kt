package com.zuko.billingz.google.store.sales

import com.android.billingclient.api.Purchase
import com.zuko.billingz.lib.store.sales.Order
import com.zuko.billingz.lib.store.sales.Receipt

data class GoogleReceipt(
    override var order: Order? = null,
    var purchase: Purchase? = null
): Receipt