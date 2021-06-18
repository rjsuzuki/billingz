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
package com.zuko.billingz.lib.store.sales

import android.app.Activity
import androidx.collection.ArrayMap
import androidx.lifecycle.MutableLiveData
import com.zuko.billingz.lib.misc.CleanUpListener
import com.zuko.billingz.lib.misc.OrderResult
import com.zuko.billingz.lib.store.client.Client
import com.zuko.billingz.lib.store.products.Product

/**
 * Blueprint of the primary behavior for selling products and the handling of orders
 */
interface Sales : CleanUpListener {

    /**
     * @see [OrderUpdateListener]
     */
    var orderUpdateListener: OrderUpdateListener?

    /**
     * @see [OrderValidatorListener]
     */
    var orderValidatorListener: OrderValidatorListener?

    /**
     * Provides a liveData [Order] object for
     * developers to observe and react to on
     * the UI/Main thread.
     * Objects can be passed from the normal purchase flow
     * or when the app is verifying a list of queried purchases.
     */
    var order: MutableLiveData<Order>

    /**
     * mutable live data object of a queried [Order]
     */
    var queriedOrder: MutableLiveData<Order>

    /**
     * @return - mutable live data observable for an [Order]
     */
    fun getOrderOrQueried(): MutableLiveData<Order>

    /**
     *
     * ArrayMap<OrderId, Order>
     */
    var pendingOrders: ArrayMap<String, Order>

    /**
     * @param activity
     * @param product
     * @param client
     * @return [OrderResult]
     */
    fun startOrderRequest(
        activity: Activity,
        product: Product,
        client: Client
    ): OrderResult

    /**
     * Handler method for responding to updates from Android's PurchaseUpdatedListener class
     * or when checking results from queryPurchases()
     * @param orderResult
     * @param orders
     * Should run on background thread.
     */
    fun processUpdatedOrders(orderResult: OrderResult?, orders: MutableList<Order>?)

    /**
     * @param order
     */
    fun processValidation(order: Order)

    /**
     * Simple validation checks before
     * allowing developer to implement their
     * validator
     */
    fun isNewPurchase(order: Order): Boolean

    /**
     * @param order
     */
    fun processInAppPurchase(order: Order)

    /**
     * @param order
     */
    fun processSubscription(order: Order)

    /**
     * @param order
     */
    fun processPendingTransaction(order: Order)

    /**
     * @param orderResult
     */
    fun processPurchasingError(orderResult: OrderResult?)

    /**
     *
     * Fetch all purchases to keep history up-to-date.
     * Network issues, multiple devices, and external purchases
     * could create untracked purchases - call this method in the
     * onCreate and onResume lifecycle events.
     */
    fun refreshOrderHistory(sales: Sales)

    /**
     * @param sales
     */
    fun queryOrders(sales: Sales)

    /**
     * @param skuType
     * @param listener
     */
    fun queryOrderHistory(skuType: String, listener: GetReceiptsListener)

    /**
     * @return - mutable list of [Receipt]'s for a particular user.
     */
    fun getReceipts(): MutableList<Receipt>

    /**
     * Purchases can be made outside of app, or finish while app is in background.
     * show in-app popup, or deliver msg to an inbox, or use an OS notification
     */

    /**
     * Set by Manager class
     */
    interface OrderUpdateListener {

        /**
         * @param order
         * @param productType
         */
        fun resumeOrder(order: Order, productType: Product.Type)
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
        fun validate(order: Order, callback: ValidatorCallback)
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
         * @param order
         */
        fun onSuccess(order: Order)

        /**
         * @param order
         */
        fun onFailure(order: Order)
    }
}
