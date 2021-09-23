package com.zuko.billingz.core.store.model

interface Orderz {

    /**
     * A unique order identifier for the transaction.
     * Amazon IAP equivalent is receiptId
     */
    var orderId: String?

    /**
     * The time the product was purchased, in milliseconds since the epoch (Jan 1, 1970)
     */
    var orderTime: Long

    /**
     * A string token that uniquely identifies a purchase for a given item and user pair
     */
    var entitlement: String?

    /**
     *
     */
    var state: State

    /**
     *
     */
    var isCancelled: Boolean

    /**
     * Subscriptions are always 1.
     * IAPs are 1+
     */
    var quantity: Int

    /**
     * Details of the order in JSON
     */
    var originalJson: String?

    /**
     * List of all products associated with this order id.
     * Minimum size of 1 - Subscriptions are always 1.
     */
    var skus: List<String>?

    enum class State {
        UNKNOWN,
        STARTING,
        VALIDATING,
        PROCESSING,
        COMPLETE,
        FAILED,
        PENDING
    }
}
