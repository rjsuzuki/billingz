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
package com.zuko.billingz.google.store.model

import androidx.collection.ArrayMap
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.zuko.billingz.core.store.model.Orderz
import com.zuko.billingz.core.store.model.Productz

/**
 * Represents an Order.
 * @property billingResult
 * @property purchase
 * @property msg - shorthand for message.
 *
 *
 */
data class GoogleOrder(
    var purchase: Purchase? = null,
    val billingResult: BillingResult?,
    val msg: String
) : Orderz {

    /**
     * An Order ID is a string that represents a financial transaction
     * on Google Play. This string is included in a receipt that is
     * emailed to the buyer.
     * You can use the Order ID to manage refunds in the used in
     * sales and payout reports.
     *
     * Order IDs are created every time a financial transaction occurs. Except for FREE products.
     *
     * Purchases are refunded if not acknowledged withing three days.
     * to confirm a purchase -> check state is purchased, not pending.
     *
     * Value always changes for a renewing subscription
     *
     * Upgrades, downgrades, replacements, and re-sign-ups all create new purchase tokens and Order IDs.
     */
    override var orderId: String? = purchase?.orderId
    override var orderTime: Long = purchase?.purchaseTime ?: -1L

    /**
     * A purchase token is a string that represents a
     * buyer's entitlement to a product on Google Play.
     * It indicates that a Google user is entitled to a
     * specific product that is represented by a SKU.
     * You can use the purchase token with the Google Play Developer API.
     *
     * Purchase tokens are generated only when a user completes a purchase flow.
     * Value remains constant for a renewing subscription
     *
     * Upgrades, downgrades, replacements, and re-sign-ups all create new purchase tokens and Order IDs.
     */
    override var entitlement: String? = purchase?.purchaseToken

    var products: Map<String, Productz.Type> = ArrayMap()

    override var skus: List<String>? = purchase?.skus
    override var state: Orderz.State = when (purchase?.purchaseState) {
        Purchase.PurchaseState.PURCHASED -> Orderz.State.PROCESSING
        Purchase.PurchaseState.PENDING -> Orderz.State.PENDING
        else -> Orderz.State.UNKNOWN
    }
    override var isCancelled: Boolean =
        Purchase.PurchaseState.UNSPECIFIED_STATE == purchase?.purchaseState
    override var quantity: Int = purchase?.quantity ?: 1
    override var originalJson: String? = purchase?.originalJson
}
