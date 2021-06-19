package com.zuko.billingz.amazon.store.sales

import com.amazon.device.iap.model.PurchaseResponse

import com.zuko.billingz.lib.store.model.Order

data class AmazonOrder(
val response: PurchaseResponse
) : Order {
    override var orderId: String? = null
    override var packageName: String? = null
    override var orderTime: Long = 0L
    override var orderToken: String? = null

    //var status/result

    init {

    }
}