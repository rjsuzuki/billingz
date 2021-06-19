package com.zuko.billingz.lib.store.model

interface Order {

    var orderId: String?

    var packageName: String?

    var orderTime: Long

    var orderToken: String?

    var product: Product?

    var status: Status

    var isCancelled: Boolean

    enum class Status {
        UNKNOWN,
        STARTING,
        PROCESSING,
        COMPLETE,
        FAILED,
        PENDING
    }
}
