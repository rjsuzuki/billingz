package com.zuko.billingz.amazon.store.model

import com.zuko.billingz.lib.store.model.Order
import com.zuko.billingz.lib.store.model.Product
import com.zuko.billingz.lib.store.model.Receipt
import java.util.*

data class AmazonReceipt(
    var iapReceipt: com.amazon.device.iap.model.Receipt?
) : Receipt {
    override var order: Order? = null
    override var receiptId: String? = null
    override var userId: String? = null
    override var orderId: String? = null
    override var sku: String? = null
    override var productType: Product.Type = Product.Type.UNKNOWN
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