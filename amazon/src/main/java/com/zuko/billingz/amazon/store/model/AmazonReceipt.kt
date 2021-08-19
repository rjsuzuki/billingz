package com.zuko.billingz.amazon.store.model

import com.zuko.billingz.core.store.model.Orderz
import com.zuko.billingz.core.store.model.Productz
import com.zuko.billingz.core.store.model.Receiptz
import java.util.*

data class AmazonReceipt(
    var iapReceipt: com.amazon.device.iap.model.Receipt?
) : Receiptz {
    override var order: Orderz? = null
    override var receiptId: String? = null
    override var userId: String? = null
    override var orderId: String? = null
    override var sku: String? = null
    override var productType: Productz.Type = Productz.Type.UNKNOWN
    override var purchaseDate: Date? = null
    override var cancelDate: Date? = null
    override var isCanceled: Boolean = false

    var marketplace: String? = null

    init {
        iapReceipt?.let { receipt ->
            receipt.receiptId
            receipt.sku
            receipt.productType
            receipt.purchaseDate
            receipt.isCanceled
            receipt.cancelDate

        }
    }
}