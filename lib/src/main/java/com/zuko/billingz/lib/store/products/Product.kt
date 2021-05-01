/*
 * Copyright 2021 rjsuzuki
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.zuko.billingz.lib.store.products

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.SkuDetails

/**
 * @author rjsuzuki
 *
 * Note: Do not use orderId to check for duplicate purchases or as a primary key in your database,
 * as not all purchases are guaranteed to generate an orderId.
 * In particular, purchases made with promo codes do not generate an orderId.
 *
 * Blueprint for GooglePlay's products for inapp-purchases
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
