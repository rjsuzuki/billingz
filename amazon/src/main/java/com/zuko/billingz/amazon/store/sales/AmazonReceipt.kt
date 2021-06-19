package com.zuko.billingz.amazon.store.sales

import com.zuko.billingz.lib.store.model.Order
import com.zuko.billingz.lib.store.model.Receipt

data class AmazonReceipt(
    var iapReceipt: com.amazon.device.iap.model.Receipt?
) : Receipt {
    override var order: Order? = null
    override var userId: String? = null
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