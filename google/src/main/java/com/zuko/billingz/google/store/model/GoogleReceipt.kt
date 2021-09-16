package com.zuko.billingz.google.store.model

import com.android.billingclient.api.Purchase
import com.zuko.billingz.core.store.model.Orderz
import com.zuko.billingz.core.store.model.Productz
import com.zuko.billingz.core.store.model.Receiptz
import java.util.*

/**
 * Google Receipts only are generated if the purchase state is complete (purchased: 1)
 * If Pending, the Order object can be queried so that the user can complete the transaction.
 */
data class GoogleReceipt(val purchase: Purchase): Receiptz {

    override var order: Orderz? = null
    override var receiptId: String? = null
    override var orderId: String? = null
    override var userId: String? = null
    override var sku: String? = null
    override var productType: Productz.Type = Productz.Type.UNKNOWN
    override var purchaseDate: Date? = null
    override var cancelDate: Date? = null
    override var isCanceled: Boolean = false
}