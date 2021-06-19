package com.zuko.billingz.google.store.model

import com.android.billingclient.api.Purchase
import com.zuko.billingz.lib.store.model.Order
import com.zuko.billingz.lib.store.model.Receipt

data class GoogleReceipt(val purchase: Purchase): Receipt {

    override var order: Order? = null

    override var userId: String?
        get() = TODO("Not yet implemented")
        set(value) {}
}