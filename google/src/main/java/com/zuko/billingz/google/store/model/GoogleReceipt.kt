package com.zuko.billingz.google.store.model

import com.android.billingclient.api.Purchase
import com.zuko.billingz.core.store.model.Orderz
import com.zuko.billingz.core.store.model.Receiptz
import java.util.Date

/**
 * Google Receipts only are generated if the purchase state is complete (purchased: 1)
 * If Pending, the Order object can be queried so that the user can complete the transaction.
 */
data class GoogleReceipt(
    override var userId: String? = null,
    override var order: Orderz? = null,
    val purchase: Purchase): Receiptz {

    override var orderId: String? = order?.orderId
    override var orderDate: Date? = null //todo convert
    override var skus: List<String>? = null

    override var cancelDate: Date? = null
    override var isCanceled: Boolean = purchase.purchaseState == Purchase.PurchaseState.UNSPECIFIED_STATE
}
