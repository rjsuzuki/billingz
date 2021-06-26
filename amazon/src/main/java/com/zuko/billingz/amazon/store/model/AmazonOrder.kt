package com.zuko.billingz.amazon.store.model

import com.amazon.device.iap.model.PurchaseResponse

import com.zuko.billingz.lib.store.model.Order
import com.zuko.billingz.lib.store.model.Product

/**
 * Represents an Order.
 * @property response
 */
data class AmazonOrder(val response: PurchaseResponse) : Order {
    override var orderId: String? = response.receipt.receiptId
    override var packageName: String? = null
    override var orderTime: Long = 0L
    override var orderToken: String? = null
    override var product: Product? = null
    override var status: Order.Status = Order.Status.UNKNOWN
    override var isCancelled: Boolean = response.receipt.isCanceled

}