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

/**
 * A pricing phase is considered a free trial if its price is 0.
 */
fun ProductDetails.PricingPhase.isFreeTrial(): Boolean {
    // A free trial is characterized by a price of 0. [5]
    return this.priceAmountMicros == 0L
}

/**
 * A pricing phase is considered a promotion (e.g., an introductory price) if it has a price
 * and is set for a finite number of billing cycles.
 */
fun ProductDetails.PricingPhase.isPromotion(): Boolean {
    // A promotion has a price and recurs for a limited time. [4]
    return this.priceAmountMicros > 0L && this.recurrenceMode == ProductDetails.RecurrenceMode.FINITE_RECURRING
}

/**
 * A pricing phase is considered the standard, recurring price (base plan) if it recurs indefinitely.
 */
fun ProductDetails.PricingPhase.isStandardPrice(): Boolean {
    // The standard price is the one that recurs indefinitely until canceled. [4]
    return this.recurrenceMode == ProductDetails.RecurrenceMode.INFINITE_RECURRING
}
