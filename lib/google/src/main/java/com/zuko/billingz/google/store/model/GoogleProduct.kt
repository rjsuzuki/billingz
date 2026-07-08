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

package com.zuko.billingz.google.store.model

import com.android.billingclient.api.ProductDetails
import com.zuko.billingz.core.misc.Logger
import com.zuko.billingz.core.store.model.Offer
import com.zuko.billingz.core.store.model.OfferDetails
import com.zuko.billingz.core.store.model.PricingInfo
import com.zuko.billingz.core.store.model.Productz
import java.util.Currency
import java.util.Locale

/**
 * @property promotion will indicate if a free trial or promo is available.
 * @property productDetails the underlying Play Billing product details.
 * https://developer.android.com/reference/com/android/billingclient/api/ProductDetails
 */
data class GoogleProduct(
    override val type: Productz.Type
) : Productz {

    /** database id **/
    var id: Int = -1
    private var productId: String? = null
    private var name: String? = null
    private var title: String? = null
    private var description: String? = null
    private var price: String? = null
    private var iconUrl: String? = null
    private var currency: Currency = Currency.getInstance(Locale.getDefault())
    private var pricingInfo: PricingInfo? = null

    @Deprecated("Use pricingInfo instead")
    private var promotion: Productz.Promotion = Productz.Promotion.NONE
    private var productDetails: ProductDetails? = null

    @Suppress("unused")
    constructor(
        productId: String?,
        name: String?,
        title: String?,
        description: String?,
        price: String?,
        iconUrl: String?,
        currency: Currency,
        pricingInfo: PricingInfo?,
        promotion: Productz.Promotion,
        type: Productz.Type
    ) : this(type) {
        this.productId = productId
        this.name = name
        this.title = title
        this.description = description
        this.price = price
        this.iconUrl = iconUrl
        this.currency = currency
        this.pricingInfo = pricingInfo
        this.promotion = promotion
    }

    /**
     * Android Billing Lib v5+
     */
    constructor(productDetails: ProductDetails, type: Productz.Type) : this(type) {
        this.productDetails = productDetails
        productId = productDetails.productId
        name = productDetails.name
        title = productDetails.title
        description = productDetails.description

        if (type == Productz.Type.SUBSCRIPTION) {
            val basePlanProduct = productDetails.subscriptionOfferDetails?.firstOrNull { it.offerTags.isEmpty() }
            val standardOfferPhase = basePlanProduct?.pricingPhases?.pricingPhaseList?.find { it.isStandardPrice() }
            price = standardOfferPhase?.formattedPrice
            currency = try {
                Currency.getInstance(basePlanProduct?.pricingPhases?.pricingPhaseList?.firstOrNull()?.priceCurrencyCode)
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to get currency code: ${e.localizedMessage}", e)
                Currency.getInstance(Locale.getDefault())
            }

            pricingInfo = PricingInfo(
                introPrice = null,
                introPricePeriod = null,
                billingPeriod = null,
                trialPeriod = null,
                subscriptionOffers = convertSubscriptionOfferDetailsTo(productDetails.subscriptionOfferDetails)
            )
            val firstAvailableOffer = productDetails.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()
            promotion = getPromotionType(firstAvailableOffer)
        } else {
            price = productDetails.oneTimePurchaseOfferDetails?.formattedPrice
            currency = try {
                Currency.getInstance(productDetails.oneTimePurchaseOfferDetails?.priceCurrencyCode)
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to get currency code: ${e.localizedMessage}", e)
                Currency.getInstance(Locale.getDefault())
            }
        }
    }

    private fun getPromotionType(phase: ProductDetails.PricingPhase?): Productz.Promotion {
        return when {
            phase?.isFreeTrial() == true -> Productz.Promotion.FREE
            phase?.isPromotion() == true -> Productz.Promotion.PROMO
            phase?.isStandardPrice() == true -> Productz.Promotion.NONE
            else -> Productz.Promotion.NONE
        }
    }
    private fun convertSubscriptionOfferDetailsTo(offers: List<ProductDetails.SubscriptionOfferDetails>?): List<OfferDetails>? {
        if (offers.isNullOrEmpty()) {
            return null
        }
        val offerDetailsList = mutableListOf<OfferDetails>()
        offers.forEach {
            val details = convertSubscriptionOfferTo(it)
            offerDetailsList.add(details)
        }
        return offerDetailsList
    }

    /**
     * Note: An offer can consist of up to two sequential pricing phases
     * (for example, a free trial followed by a discounted price) before it eventually transitions to the
     * base plan's standard pricing. We can expect a list with a max size of 3.
     * - [Reference](https://support.google.com/googleplay/android-developer/answer/12154973?hl=en#:~:text=Offers%20contain%20one%20or%20more,discount%20off%20the%20base%20price)
     */
    private fun convertSubscriptionOfferTo(offer: ProductDetails.SubscriptionOfferDetails): OfferDetails {
        val offers = mutableListOf<Offer>()
        offer.pricingPhases.pricingPhaseList.forEach { pricingPhase ->
            val o = convertPricingPhaseTo(pricingPhase)
            offers.add(o)
        }
        return OfferDetails(
            offerId = offer.offerId,
            basePlanId = offer.basePlanId,
            offerTags = offer.offerTags,
            offerToken = offer.offerToken,
            offers = offers
        )
    }
    private fun convertPricingPhaseTo(p: ProductDetails.PricingPhase): Offer {
        return Offer(
            billingPeriod = p.billingPeriod,
            formattedPrice = p.formattedPrice,
            priceCurrencyCode = p.priceCurrencyCode,
            priceAmountMicros = p.priceAmountMicros,
            recurrenceMode = p.recurrenceMode,
            billingCycleCount = p.billingCycleCount,
            promo = getPromotionType(p)
        )
    }

    override fun getProductId(): String? {
        return productId
    }

    override fun getName(): String? {
        return name
    }

    override fun getTitle(): String? {
        return title
    }

    override fun getPrice(): String? {
        return price
    }

    override fun getDescription(): String? {
        return description
    }

    override fun getIconUrl(): String? {
        return iconUrl
    }

    override fun getPromotion(): Productz.Promotion {
        return promotion
    }

    override fun getPricingInfo(): PricingInfo? {
        return pricingInfo
    }

    override fun getCurrency(): Currency {
        return currency
    }

    override fun isAmazon(): Boolean {
        return false
    }

    override fun isGoogle(): Boolean {
        return true
    }

    /**
     * Convenience method to fetch original [ProductDetails] object.
     * Object is only available when returned from a library call.
     */
    @Suppress("unused")
    fun getProductDetails(): ProductDetails? {
        return productDetails
    }

    companion object {
        private const val TAG = "BillingzGoogleProduct"
    }
}
