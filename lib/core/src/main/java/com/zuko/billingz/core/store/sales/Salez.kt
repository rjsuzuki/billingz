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

package com.zuko.billingz.core.store.sales

import android.app.Activity
import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import com.zuko.billingz.core.misc.CleanUpz
import com.zuko.billingz.core.store.client.Clientz
import com.zuko.billingz.core.store.model.OrderHistoryz
import com.zuko.billingz.core.store.model.Orderz
import com.zuko.billingz.core.store.model.Productz
import com.zuko.billingz.core.store.model.QueryResult
import com.zuko.billingz.core.store.model.Receiptz
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface Salez : CleanUpz {
    /**
     * For providing version compatability changes
     */
    var isNewVersion: Boolean

    val currentOrder: MutableLiveData<Orderz>
    /**
     * Provides a liveData [Orderz] object for
     * developers to observe and react to on
     * the UI/Main thread.
     * Objects can be passed from the normal purchase flow
     * or when the app is verifying a list of queried purchases.
     */
    val currentReceipt: MutableLiveData<out Receiptz>

    /**
     * Collection of the latest purchases sorted as Key/Value pairs.
     * Key = string value of entitlement id(purchase token for Google, receipt id for Amazon)
     *
     */
    val orderHistoryLiveData: MutableLiveData<out OrderHistoryz>
    val orderHistoryStateFlow: MutableStateFlow<out OrderHistoryz?>
    val orderHistoryState: StateFlow<OrderHistoryz?>
    /**
     * Callback method required to be implemented by developer
     */
    var orderUpdaterListener: OrderUpdaterListener?

    /**
     * Callback method required to be implemented by developer
     */
    var orderValidatorListener: OrderValidatorListener?

    /**
     * Start a basic purchase flow
     */
    fun startOrder(
        activity: Activity?,
        product: Productz,
        client: Clientz,
        options: Bundle? = null
    )

    /**
     *  Verify the order(purchase). Check for fraud/abuse.
     */
    fun validateOrder(order: Orderz)

    /**
     * Internal use only
     */
    fun processOrder(order: Orderz)

    /**
     * Give content to the user.
     * Acknowledge delivery of content. Optionally, mark the item as consumed
     * so that the user can buy the item again.
     * Internal use only
     */
    fun completeOrder(order: Orderz)

    /**
     * Internal use only
     */
    fun cancelOrder(order: Orderz)

    /**
     * Purchases can be voided for a variety of reasons, such as:
     * 1. A purchase is canceled, either by the user, by the developer, or by Google.
     *      For subscriptions, note that this refers to canceling the purchase of a subscription,
     *      rather than canceling the subscription itself.
     * 2. A purchase is charged back.
     * 3. The app developer cancels or refunds a user order and checks the
     *      "revoke" option in the console.
     */
    fun failedOrder(order: Orderz)

    /**
     * Internal use only
     */
    fun refreshQueries()

    /**
     * Returns purchases details for currently owned items bought within your app.
     * Only active subscriptions and non-consumed one-time purchases are returned.
     * This method uses a cache of Google Play Store app without initiating a network request.
     * Note: It's recommended for security purposes to go through purchases verification
     * on your backend (if you have one) by calling one of the following APIs:
     * [https://developers.google.com/android-publisher/api-ref/purchases/products/get]
     * [https://developers.google.com/android-publisher/api-ref/purchases/subscriptions/get]
     */
    fun queryOrders(): QueryResult<Orderz>

    /**
     * Returns the most recent purchase made by the user for each SKU,
     * even if that purchase is expired, canceled, or consumed.
     */
    fun queryReceipts(type: Productz.Type? = null): QueryResult<OrderHistoryz>

    /**
     * @param profileId - Specifies an optional obfuscated string that is
     * uniquely associated with the user's profile in your app
     * @param accountId - Google Play can use it to detect irregular activity,
     * such as many devices making purchases on the same account in a
     * short period of time (64 character limit)
     * https://developer.android.com/reference/com/android/billingclient/api/AccountIdentifiers#getObfuscatedAccountId()
     */
    fun setObfuscatedIdentifiers(accountId: String? = null, profileId: String? = null)

    /**
     * For developers to implement.
     * Enables developer to provide another verification step before finalizing an order. Also,
     * Purchases can be made outside of app, or finish while app is in background, and may not have
     * completed in a regular ui-flow and requires attention again.
     * show in-app popup, or deliver msg to an inbox, or use an OS notification.
     */
    interface OrderUpdaterListener {
        /**
         *
         */
        fun onComplete(receipt: Receiptz)

        fun onFailure(order: Orderz)
    }

    /**
     * For developers to implement.
     * Enables the ability to verify purchases with your own logic,
     * ensure entitlement was not already granted for this purchaseToken,
     * and grant entitlement to the user.
     */
    interface OrderValidatorListener {

        /**
         * @param order
         * @param callback
         */
        fun validate(order: Orderz, callback: ValidatorCallback)
    }

    /**
     * Respond to the events triggered by the developer's validator.
     * Developers will need to implement this interface if custom validation checks
     * need to be provided before finalizing an order.
     * If the purchase is properly verified, call onSuccess,
     * otherwise call onFailure so the library can appropriately continue the
     * lifecycle of a customer's order.
     */
    interface ValidatorCallback {

        /**
         * Developers should verify the order with their own backend records of a users purchase
         * history prior to calling this method.
         * @param order
         */
        fun validated(order: Orderz)

        /**
         * Call if order is deemed invalid due to the nature of the purchase. i.e. the order was
         * fulfilled already or the sku is no longer available, etc.
         * @param order
         */
        fun invalidated(order: Orderz)
    }
}
