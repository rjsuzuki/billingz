package com.zuko.billingz.lib.products

import androidx.lifecycle.MutableLiveData
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.SkuDetails
import com.zuko.billingz.lib.sales.Order
import kotlinx.coroutines.CoroutineScope


/**
 * @author rjsuzuki
 *
 * Note: Do not use orderId to check for duplicate purchases or as a primary key in your database,
 * as not all purchases are guaranteed to generate an orderId.
 * In particular, purchases made with promo codes do not generate an orderId.
 */
interface Product {

    /**
     * A convenience variable for database management.
     * You may need to override each class to provide annotations for Room
     */
    var id: Int?

    /**
     * The sku product id that is referenced by your Google Play account
     */
    var sku: String?

    /**
     * A convenience variable to provide the name of your product
     */
    var name: String?

    /**
     * A convenience variable to provide the price of your product as a String
     */
    var price: String?

    /**
     * A convenience variable to provide a description of your product
     */
    var description: String?

    /**
     * @see [SkuDetails]
     */
    var details: SkuDetails?

    enum class ProductType {
        FREE_CONSUMABLE,
        FREE_SUBSCRIPTION,
        FREE_NON_CONSUMABLE,
        CONSUMABLE,
        NON_CONSUMABLE,
        SUBSCRIPTION,
        PROMO_CONSUMABLE,
        PROMO_NON_CONSUMABLE,
        PROMO_SUBSCRIPTION,
        ALL
    }

    /**
     * @see [BillingClient.SkuType]
     */
    val skuType: String

    /**
     *
     */
    val type: ProductType


}