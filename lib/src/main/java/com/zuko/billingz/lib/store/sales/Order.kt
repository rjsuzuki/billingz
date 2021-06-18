package com.zuko.billingz.lib.store.sales

interface Order {

    var orderId: String?

    var packageName: String?

    var orderTime: Long?

    var orderToken: String?
}
