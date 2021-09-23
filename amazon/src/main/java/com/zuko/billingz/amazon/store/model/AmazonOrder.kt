package com.zuko.billingz.amazon.store.model

import com.amazon.device.iap.model.Product
import com.amazon.device.iap.model.PurchaseResponse

import com.zuko.billingz.core.store.model.Orderz
import com.zuko.billingz.core.store.model.Productz
import java.util.UUID

/**
 * Represents an Order.
 * @property response
 */
data class AmazonOrder(val response: PurchaseResponse) : Orderz {

    var product: Productz? = null

    override var orderId: String? = response.receipt.receiptId
    override var orderTime: Long = response.receipt.purchaseDate.time

    // UUID.randomUUID().toString()
    // create a way to determine if amazon product has been granted entitlement
    override var entitlement: String? = null
    override var skus: List<String>? = listOf(response.receipt.sku)
    override var state: Orderz.State = Orderz.State.UNKNOWN
    override var isCancelled: Boolean = response.receipt.isCanceled
    override var quantity: Int = 1
    override var originalJson: String? = response.toJSON().toString()
}