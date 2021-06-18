package com.zuko.billingz.amazon.store.sales

import com.zuko.billingz.lib.store.sales.Order
import com.zuko.billingz.lib.store.sales.Receipt

class AmazonReceipt(override var order: Order?) : Receipt {
    var iapReceipt: com.amazon.device.iap.model.Receipt? = null
}