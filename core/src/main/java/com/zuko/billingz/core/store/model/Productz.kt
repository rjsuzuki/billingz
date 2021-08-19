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
package com.zuko.billingz.core.store.model

/**
 * @author rjsuzuki
 *
 * Note: Do not use orderId to check for duplicate purchases or as a primary key in your database,
 * as not all purchases are guaranteed to generate an orderId.
 * In particular, purchases made with promo codes do not generate an orderId.
 *
 */
interface Productz {

    /**
     * A convenience variable for database management.
     * You may need to override each class to provide annotations for Room
     */
    var id: Int?

    /**
     * The sku product id that is referenced by your account
     */
    var sku: String?

    /**
     * A convenience variable to provide the name/title of your product
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
     * The remote path of the products image
     */
    var iconUrl: String?

    /**
     * The product type can be one of the following:
     * CONSUMABLE,
     * NON_CONSUMABLE (aka Entitlement),
     * SUBSCRIPTION
     */
    val type: Type

    /**
     * The promotion type of the product.
     * NONE is the default.
     */
    val promotion: Promotion

    enum class Type {
        UNKNOWN,
        CONSUMABLE,
        NON_CONSUMABLE,
        SUBSCRIPTION,
    }

    enum class Promotion {

        /**
         * No promotion is attached to the product.
         * Default value.
         */
        NONE,

        /**
         * A product that is free of charge to purchase.
         */
        FREE,

        /**
         * A product on sale or a special release of some sort.
         */
        PROMO
    }
}
