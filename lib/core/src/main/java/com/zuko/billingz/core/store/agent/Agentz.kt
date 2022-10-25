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
package com.zuko.billingz.core.store.agent

import android.app.Activity
import android.os.Bundle
import androidx.annotation.UiThread
import androidx.lifecycle.LiveData
import com.zuko.billingz.core.store.client.Clientz
import com.zuko.billingz.core.store.model.OrderHistoryz
import com.zuko.billingz.core.store.model.Orderz
import com.zuko.billingz.core.store.model.Productz
import com.zuko.billingz.core.store.model.QueryResult
import com.zuko.billingz.core.store.sales.Salez
import kotlinx.coroutines.flow.StateFlow

/**
 * Facade pattern - a simple interface for interacting with the main features of
 * the Google Billing Library
 * @author rjsuzuki
 */
interface Agentz {

    /**
     * Update the current library instance with new user identifiers.
     */
    fun updateIdentifiers(accountId: String?, profileId: String?, hashingSalt: String?)

    /**
     * Checks if the billing client has products loaded. if false, use [queryInventory]
     * to prepare the client for purchases.
     */
    @UiThread
    fun isInventoryReadyLiveData(): LiveData<Boolean>

    fun isInventoryReadyStateFlow(): StateFlow<Boolean>

    /**
     * Observe changes to the BillingClient's connection to GooglePlay
     * from the UI thread (in an activity/fragment class). Calling this function
     * automatically checks if client is connected and will attempt to reconnect
     * if need be.
     * @return [LiveData<Boolean>]
     */
    @UiThread
    fun getState(): LiveData<Clientz.ConnectionStatus>

    /**
     * Initiate the purchase flow from the perspective of a user interaction.
     * e.g. a Customer opens your app and selects a product for purchase.
     * @return [LiveData<Order>]
     * @param activity - the currently active android Activity class
     * @param productId - the product id that can be found on the GooglePlayConsole
     * validation of a customer's purchase order - this allows you to do such things as verifying
     * a purchase with your backend before completing the purchase flow.
     */
    @UiThread
    fun startOrder(
        activity: Activity?,
        productId: String?,
        options: Bundle? = null
    ): LiveData<Orderz>

    /**
     * Returns the available history of 'completed orders' as a mapping of receipt objects.
     * The receipts will list all relevant skus, even if that order is expired, canceled, or consumed.
     * Set parameter to null for all products
     */
    fun queryReceipts(type: Productz.Type?): QueryResult<OrderHistoryz>

    /**
     * This function should be called in the onResume lifecycle of the application/view.
     * Amazon and Google Play use different terminology for similar use-cases,
     * so in order to prevent confusion, this library will always define an Order as a purchase in-progress,
     * and/or incomplete. A receipt will always be a 'completed order' (expired, canceled, or consumed).
     * Handle purchases still remaining from recent history. Observe the liveData object
     * that will emit [Orderz] objects that require your attention.
     * - These orders/purchases could be purchases made on another device, or when the app is
     * resuming, etc.
     * @return [QueryResult] - observe either the LiveData or StateFlow object for any use-cases
     * that are not specific to the validation process.
     * - The provided listeners will enable you to resume the processing (validation) of an Order.
     * @see [Salez.OrderValidatorListener]
     * @see [Salez.OrderUpdaterListener]
     *
     */
    fun queryOrders(): QueryResult<Orderz>

    /**
     * Queries database for matching product ids and loads them into the inventory cache.
     * @param products - key is the sku(product id), value is the product type
     */
    fun queryInventory(products: Map<String, Productz.Type>): QueryResult<Map<String, Productz>>

    /**
     *
     * TODO: Implement cache-first strategy and boolean bypass parameter for it.
     */
    fun queryProduct(sku: String, type: Productz.Type): QueryResult<Productz>

    /**
     * Return a product that matches the sku (product id).
     * Null if no matching product exists.
     * @see queryInventory or [queryProduct] should be used instead.
     */
    @Deprecated("Will be removed in a future release.")
    fun getProduct(sku: String?): Productz?

    /**
     * Return a list of validated products from the inventory.
     * Narrow your search results by providing both or either parameters.
     * @see queryInventory or [queryProduct] should be used instead.
     */
    @Deprecated("Will be removed in a future release")
    fun getProducts(type: Productz.Type?, promo: Productz.Promotion?): Map<String, Productz>

    /**
     * A manual option to complete an existing Order. This will call the respective function to
     * consume, acknowledge, and/or fulfill a product.
     */
    fun completeOrder(order: Orderz)

    /**
     *  A manual option to explicitly cancel an order
     */
    fun cancelOrder(order: Orderz)
}
