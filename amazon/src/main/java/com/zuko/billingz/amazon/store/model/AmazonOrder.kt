package com.zuko.billingz.amazon.store.model

import com.amazon.device.iap.model.PurchaseResponse

import com.zuko.billingz.core.store.model.Orderz
import com.zuko.billingz.core.store.model.Productz

/**
 * Represents an Order.
 * @property response
 */
data class AmazonOrder(val response: PurchaseResponse) : Orderz {
    override var orderId: String? = response.receipt.receiptId
    override var packageName: String? = null
    override var orderTime: Long = 0L
    override var orderToken: String? = null
    override var product: Productz? = null
    override var status: Orderz.Status = Orderz.Status.UNKNOWN
    override var isCancelled: Boolean = response.receipt.isCanceled

}