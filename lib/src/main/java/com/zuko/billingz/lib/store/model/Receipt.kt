package com.zuko.billingz.lib.store.model

import java.util.*


interface Receipt {

    var receiptId: String?
    var sku: String?

    var purchaseDate: Date?
    var cancelData: Date?

    var userId: String?
    var order: Order?

}
