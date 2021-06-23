package com.zuko.billingz.amazon.store.model

import com.zuko.billingz.lib.store.model.Order
import com.zuko.billingz.lib.store.model.Receipt
import java.util.*

data class AmazonReceipt(
    var iapReceipt: com.amazon.device.iap.model.Receipt?
) : Receipt {
    override var order: Order? = null
    override var receiptId: String?
        get() = TODO("Not yet implemented")
        set(value) {}
    override var sku: String?
        get() = TODO("Not yet implemented")
        set(value) {}
    override var purchaseDate: Date?
        get() = TODO("Not yet implemented")
        set(value) {}
    override var cancelData: Date?
        get() = TODO("Not yet implemented")
        set(value) {}
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