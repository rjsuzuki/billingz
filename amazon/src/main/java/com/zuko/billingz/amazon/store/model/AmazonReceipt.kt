package com.zuko.billingz.amazon.store.model

import com.amazon.device.iap.model.Receipt
import com.zuko.billingz.core.store.model.Orderz
import com.zuko.billingz.core.store.model.Receiptz
import java.util.Date

data class AmazonReceipt(
    var iapReceipt: Receipt,
    override var userId: String? = null,
    override var order: Orderz? = null
) : Receiptz {

    override var orderId: String? = iapReceipt.receiptId
    override var orderDate: Date? = iapReceipt.purchaseDate
    override var skus: List<String>? = listOf(iapReceipt.sku)

    override var cancelDate: Date? = iapReceipt.cancelDate
    override var isCanceled: Boolean = iapReceipt.isCanceled

    var marketplace: String? = null
}