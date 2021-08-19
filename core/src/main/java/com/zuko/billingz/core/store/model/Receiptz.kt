package com.zuko.billingz.core.store.model

import java.util.Date

interface Receiptz {

    /**
     * Unique id for receipt.
     * IF amazon - the receiptId is the same is this var
     *
     */
    var receiptId: String?

    /**
     * Unique id of the order
     */
    var orderId: String?

    /**
     * unique id of the user as identified by the relevant billing service.
     */
    var userId: String?

    /**
     * Product id
     */
    var sku: String?

    /**
     * @see [Productz.Type]
     */
    var productType: Productz.Type

    /**
     * Date of purchase
     */
    var purchaseDate: Date?

    /**
     * Date of cancellation,
     * null if not available.
     */
    var cancelDate: Date?

    /**
     *
     */
    var isCanceled: Boolean

    // todo remove
    var order: Orderz?
}
