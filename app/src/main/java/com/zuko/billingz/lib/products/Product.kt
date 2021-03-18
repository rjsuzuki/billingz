package com.zuko.billingz.lib.products

import androidx.lifecycle.MutableLiveData
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase
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

    enum class SkuType { INAPP, SUB }

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

    val type: ProductType

    fun completeOrder(billingClient: BillingClient?,
                      purchase: Purchase,
                      order: MutableLiveData<Order>,
                      mainScope: CoroutineScope? = null)
}