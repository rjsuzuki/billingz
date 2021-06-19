package com.zuko.billingz.amazon.store.sales

import com.amazon.device.iap.model.PurchaseResponse

import com.zuko.billingz.lib.store.model.Order
import com.zuko.billingz.lib.store.model.Product

data class AmazonOrder(
val response: PurchaseResponse
) : Order {
    override var orderId: String? = null
    override var packageName: String? = null
    override var orderTime: Long = 0L
    override var orderToken: String? = null
    override var product: Product?
        get() = TODO("Not yet implemented")
        set(value) {}
    override var status: Order.Status
        get() = TODO("Not yet implemented")
        set(value) {}
    override var isCancelled: Boolean
        get() = TODO("Not yet implemented")
        set(value) {}

    //var status/result

    init {

    }
}