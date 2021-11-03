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
package com.zuko.billingz.core.store.agent

import android.app.Activity
import android.os.Bundle
import androidx.annotation.UiThread
import androidx.collection.ArrayMap
import androidx.lifecycle.LiveData
import com.zuko.billingz.core.store.client.Clientz
import com.zuko.billingz.core.store.model.Orderz
import com.zuko.billingz.core.store.model.Productz
import com.zuko.billingz.core.store.model.Receiptz
import com.zuko.billingz.core.store.sales.Salez

/**
 * Facade pattern - a simple interface for interacting with the main features of
 * the Google Billing Library
 * @author rjsuzuki
 */
interface Agentz {

    /**
     * Checks if the billing client has products loaded. if false, use [updateInventory]
     * to prepare the client for purchases.
     */
    @UiThread
    fun isInventoryReady(): LiveData<Boolean>

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
     * @param listener - @see [Salez.OrderValidatorListener] a callback function to enable customized
     * validation of a customer's purchase order - this allows you to do such things as verifying
     * a purchase with your backend before completing the purchase flow.
     */
    @UiThread
    fun startOrder(
        activity: Activity?,
        productId: String?,
        options: Bundle? = null,
        listener: Salez.OrderValidatorListener?
    ): LiveData<Orderz>

    /**
     * Returns the most recent purchase(s) made
     * by the user for each SKU, even if that purchase is expired, canceled, or consumed.
     * Set parameter to null for all products
     */
    @UiThread
    fun queryReceipts(type: Productz.Type?): LiveData<ArrayMap<String, Receiptz>>

    /**
     * Handle purchases still remaining from recent history. Observe the liveData object
     * that will emit [Orderz] objects that require your attention. These orders/purchases
     * could be purchases made on another device, or when the app is resuming, etc. The
     * provided listeners will enable you to resume the processing of an Order.
     * @see [Salez.OrderValidatorListener]
     * @see [Salez.OrderUpdaterListener]
     */
    @UiThread
    fun queryOrders(): LiveData<Orderz>

    /**
     * Queries database for matching product ids and loads them into the inventory.
     * @param products - key is the sku(product id), value is the product type
     */
    fun updateInventory(products: Map<String, Productz.Type>): LiveData<Map<String, Productz>>

    /**
     * Return a product that matches the sku (product id).
     * Null if no matching product exists.
     */
    fun getProduct(sku: String?): Productz?

    /**
     * Return a list of validated products from the inventory.
     * Narrow your search results by providing both or either parameters.
     */
    fun getProducts(type: Productz.Type?, promo: Productz.Promotion?): Map<String, Productz>
}
