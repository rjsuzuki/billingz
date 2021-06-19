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
package com.zuko.billingz.lib.store.agent

import android.app.Activity
import androidx.annotation.UiThread
import androidx.lifecycle.LiveData
import com.zuko.billingz.lib.store.model.Product
import com.zuko.billingz.lib.store.model.Order
import com.zuko.billingz.lib.store.model.Receipt
import com.zuko.billingz.lib.store.sales.Sales

/**
 * Facade pattern - a simple interface for interacting with the main features of
 * the Google Billing Library
 * @author rjsuzuki
 */
interface Agent {

    /**
     * Observe changes to the BillingClient's connection to GooglePlay
     * from the UI thread (in an activity/fragment class).
     * @return [LiveData<Boolean>]
     */
    @UiThread
    fun isBillingClientReady(): LiveData<Boolean>

    /**
     * Initiate the purchase flow from the perspective of a user interaction.
     * e.g. a Customer opens your app and selects a product for purchase.
     * @return [LiveData<Order>]
     * @param activity - the currently active android Activity class
     * @param productId - the product id that can be found on the GooglePlayConsole
     * @param listener - @see [Sales2.OrderValidatorListener] a callback function to enable customized
     * validation of a customer's purchase order - this allows you to do such things as verifying
     * a purchase with your backend before completing the purchase flow.
     */
    @UiThread
    fun startOrder(activity: Activity?,
                   productId: String?,
                   listener: Sales.OrderValidatorListener?): LiveData<Order>

    /**
     * Returns the most recent purchase(s) made
     * by the user for each SKU, even if that purchase is expired, canceled, or consumed.
     * Set parameter to null for all products
     */
    fun getReceipts(type: Product.Type?): LiveData<List<Receipt>>

    /**
     * Handle purchases still remaining from recent history. Observe the liveData object
     * that will emit [Order] objects that require your attention. These orders/purchases
     * could be purchases made on another device, or when the app is resuming, etc. The
     * provided listeners will enable you to resume the processing of an Order.
     * @see [Sales.OrderValidatorListener]
     * @see [Sales.OrderUpdaterListener]
     */
    fun queryOrders()

    /**
     * Queries database for matching product ids and loads them into the inventory.
     */
    fun updateInventory(skuList: List<String>, type: Product.Type)

    /**
     * Return a list of validated products from the inventory.
     * Narrow your search results by providing both or either parameters.
     */
    fun getProducts(type: Product.Type?, promo: Product.Promotion?): List<Product>

    /**
     * Return a product that matches the sku (product id).
     * Null if no matching product exists.
     */
    fun getProduct(sku: String): Product?

}
