package com.zuko.billingz.lib.store.model

import java.util.*


interface Receipt {

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
     * @see [Product.Type]
     */
    var productType: Product.Type

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
    var order: Order?

}
