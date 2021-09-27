package com.zuko.billingz.google.store.model

import com.android.billingclient.api.Purchase
import com.zuko.billingz.core.store.model.Orderz
import com.zuko.billingz.core.store.model.Receiptz
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Google Receipts only are generated if the purchase state is complete (purchased: 1)
 * If Pending, the Order object can be queried so that the user can complete the transaction.
 */
data class GoogleReceipt(
    val purchase: Purchase?,
    override var userId: String? = null,
    override var order: Orderz? = null
): Receiptz {
    override var entitlement: String? = purchase?.purchaseToken
    override var orderId: String? = order?.orderId
    override var orderDate: Date? = order?.orderTime?.let { Date(it) }
    override var skus: List<String>? = order?.skus

    override var cancelDate: Date? = null
    override var isCanceled: Boolean = purchase?.purchaseState == Purchase.PurchaseState.UNSPECIFIED_STATE

    var quantity: Int = 1
    var signature: String? = purchase?.signature
    var originalJson: String? = purchase?.originalJson
}
