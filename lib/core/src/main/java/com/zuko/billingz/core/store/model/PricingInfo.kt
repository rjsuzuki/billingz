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

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 *
 * - For Google Play Billing v4-, when using the [com.android.billingclient.api.SkuDetails],
 * the [subscriptionOffers] property will be null.
 * - For Google Play Billing v5+, when using the [com.android.billingclient.api.ProductDetails],
 * the String properties in this class will be null.
 * - If you want to access other available offers, use [subscriptionOffers] to fetch the full list.
 * https://developer.android.com/reference/com/android/billingclient/api/ProductDetails
 */
@Parcelize
data class PricingInfo(
    @Deprecated("Use subscriptionOffers instead when using Google Play Billing v5+")
    override val introPrice: String?,
    @Deprecated("Use subscriptionOffers instead when using Google Play Billing v5+")
    override val introPricePeriod: String?,
    @Deprecated("Use subscriptionOffers instead when using Google Play Billing v5+")
    override val billingPeriod: String?,
    @Deprecated("Use subscriptionOffers instead when using Google Play Billing v5+")
    override val trialPeriod: String?,
    override val subscriptionOffers: List<OfferDetails>?
) : Productz.Pricing, Parcelable
