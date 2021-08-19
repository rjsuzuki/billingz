package com.zuko.billingz.core.store.model

interface Orderz {

    var orderId: String?

    var packageName: String?

    var orderTime: Long
    var orderToken: String?

    var product: Productz?

    var status: Status

    var isCancelled: Boolean

    enum class Status {
        UNKNOWN,
        STARTING,
        VALIDATING,
        PROCESSING,
        COMPLETE,
        FAILED,
        PENDING
    }
}
