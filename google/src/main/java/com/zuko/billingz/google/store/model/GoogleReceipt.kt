package com.zuko.billingz.google.store.model

import com.android.billingclient.api.Purchase
import com.zuko.billingz.lib.store.model.Order
import com.zuko.billingz.lib.store.model.Product
import com.zuko.billingz.lib.store.model.Receipt
import java.util.*

/**
 * Google Receipts only are generated if the purchase state is complete (purchased: 1)
 * If Pending, the Order object can be queried so that the user can complete the transaction.
 */
data class GoogleReceipt(val purchase: Purchase): Receipt {

    override var order: Order? = null
    override var receiptId: String? = null
    override var orderId: String? = null
    override var userId: String? = null
    override var sku: String? = null
    override var productType: Product.Type = Product.Type.UNKNOWN
    override var purchaseDate: Date? = null
    override var cancelDate: Date? = null
    override var isCanceled: Boolean = false
}