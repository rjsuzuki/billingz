package com.zuko.billingz.core.store.model

import java.util.Date

interface Receiptz {

    /**
     * Unique id for granting entitlement of a product.
     * Google uses PurchaseTokens.
     */
    var entitlement: String?

    /**
     * Unique id of the order
     */
    var orderId: String?

    /**
     * Unique id of the user as identified by the relevant billing service.
     * Amazon only has a userData object, but Google will either provide an accountId
     * and/or profileId
     */
    var userId: String?

    /**
     * Product id
     */
    var skus: List<String>?

    /**
     * Date of purchase
     */
    var orderDate: Date?

    /**
     * Date of cancellation,
     * null if not available.
     */
    var cancelDate: Date?

    /**
     *
     */
    var isCanceled: Boolean


    var order: Orderz?
}
