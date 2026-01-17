/*
 *
 *  * Copyright 2021 rjsuzuki
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  *
 *
 */
package com.zuko.billingz.core.store.model

import com.zuko.billingz.core.misc.ModuleIdentifier
import java.util.Currency

/**
 * @author rjsuzuki
 *
 * Note: Do not use orderId to check for duplicate purchases or as a primary key in your database,
 * as not all purchases are guaranteed to generate an orderId.
 * In particular, purchases made with promo codes do not generate an orderId.
 *
 */
interface Productz : ModuleIdentifier {

    /**
     * The sku (product id) that is referenced by your account's store listing
     */
    fun getProductId(): String?

    /**
     * A convenience variable to provide the name of your product.
     * Value may be the same as [getTitle].
     */
    fun getName(): String?

    /**
     * A convenience variable to provide the title of your product
     * Value may be the same as [getName].
     */
    fun getTitle(): String?

    /**
     * A convenience variable to provide the price of your product as a String
     * in the local currency
     */
    fun getPrice(): String?

    /**
     * A convenience variable to provide a description of your product
     */
    fun getDescription(): String?

    /**
     * The remote path of the products image.
     * Android Billing Lib v5 does not support this.
     */
    fun getIconUrl(): String?

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
    fun getPromotion(): Promotion

    /**
     * Amazon IAP does not support these particular data points, so this object will return null
     * for the Amazon flavor.
     */
    fun getPricingInfo(): PricingInfo?

    /**
     * @return - [Currency] object to represent the ISO 4217 currency code
     * for price and original price.
     * Defaults to [java.util.Locale]
     */
    fun getCurrency(): Currency

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

    /**
     * Supplementary pricing information on a product if available.
     */
    interface Pricing {
        val introPrice: String?
        val introPricePeriod: String?
        val billingPeriod: String?
        val trialPeriod: String?

        /**
         * Only available for Google products. Amazon products will return null.
         */
        val subscriptionOffers: List<OfferDetails>?
    }

    /**
     * For Google Play SubscriptionOfferDetails support
     */
    interface OfferDetails {
        val offerId: String?
        val basePlanId: String
        val offerTags: List<String>
        val offerToken: String
        val offers: List<Offer>
    }

    /**
     * For Google Play SubscriptionOfferDetails support
     */
    interface Offer {
        val billingPeriod: String
        val formattedPrice: String
        val priceCurrencyCode: String
        val priceAmountMicros: Long
        val recurrenceMode: Int
        val billingCycleCount: Int
    }
}
