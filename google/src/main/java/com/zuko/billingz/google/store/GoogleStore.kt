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
package com.zuko.billingz.google.store

import android.app.Activity
import android.content.Context
import androidx.lifecycle.LiveData
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.PurchasesUpdatedListener
import com.zuko.billingz.google.store.client.GoogleClient
import com.zuko.billingz.google.store.inventory.GoogleInventory
import com.zuko.billingz.google.store.model.GoogleOrder
import com.zuko.billingz.lib.LogUtil
import com.zuko.billingz.lib.store.agent.Agent
import com.zuko.billingz.lib.store.inventory.Inventory
import com.zuko.billingz.lib.store.model.Product
import com.zuko.billingz.google.store.sales.GoogleSales
import com.zuko.billingz.lib.store.Store
import com.zuko.billingz.lib.store.client.Client
import com.zuko.billingz.lib.store.model.Order
import com.zuko.billingz.lib.store.model.Receipt
import com.zuko.billingz.lib.store.sales.Sales
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel

/**
 * @author rjsuzuki
 * //TODO handle pending purchases?
 * //TODO retry connection
 */
class GoogleStore private constructor(): Store {

    private val mainScope = MainScope()
    private val purchasesUpdatedListener: PurchasesUpdatedListener =
        PurchasesUpdatedListener { billingResult, purchases -> (sales as GoogleSales).processUpdatedPurchases(billingResult, purchases) }
    private val connectionListener = object : Client.ConnectionListener {
        override fun connected() {
            sales.refreshQueries()
        }
    }

    private val client: Client = GoogleClient(purchasesUpdatedListener)
    private val inventory: Inventory = GoogleInventory(client as GoogleClient)
    private val sales: Sales = GoogleSales(inventory as GoogleInventory, client as GoogleClient)

    private var isInitialized = false


    /*****************************************************************************************************
     * Lifecycle events - developer must either add this class to a lifecycleOwner or manually add the events
     * to their respective parent view
     *****************************************************************************************************/
    init {
        LogUtil.log.v(TAG, "instantiating...")
    }

    override fun init(context: Context?) {
        LogUtil.log.v(TAG, "initializing...")
        client.init(context, connectionListener)
        isInitialized = true
    }

    override fun create() {
        LogUtil.log.v(TAG, "creating...")
        if (isInitialized && client.initialized())
            client.connect()

        if(client.isReady())
            sales.refreshQueries() // might need to react to connection
    }

    override fun start() {
        LogUtil.log.v(TAG, "starting...")
    }

    override fun resume() {
        LogUtil.log.v(TAG, "resuming...")
        client.checkConnection()
        if(client.isReady())
            sales.refreshQueries()
    }

    override fun pause() {
        LogUtil.log.v(TAG, "pausing...")
    }

    override fun stop() {
        LogUtil.log.v(TAG, "stopping...")
        client.disconnect()
    }

    override fun destroy() {
        LogUtil.log.v(TAG, "destroying...")
        client.destroy()
        sales.destroy()
        inventory.destroy()
        mainScope.cancel()
    }

    /*****************************************************************************************************
     * Private methods
     *****************************************************************************************************/

    private val storeAgent = object : Agent {

        override fun isBillingClientReady(): LiveData<Boolean> {
            return client.isClientReady
        }

        override fun startOrder(
            activity: Activity?,
            productId: String?,
            listener: Sales.OrderValidatorListener?
        ): LiveData<Order> {
            LogUtil.log.v(TAG, "Starting purchase flow")

            sales.orderValidatorListener = listener

            val product = inventory.allProducts[productId]

            product?.let {
                sales.startOrder(activity, product, client)
                val order = GoogleOrder(
                    billingResult = null,
                    msg = "Processing..."
                )
                sales.currentOrder.postValue(order)
            } ?: sales.currentOrder.postValue(GoogleOrder(
                billingResult = null,
                msg = "Product: $productId not found."
            ))
            return sales.currentOrder
        }

        override fun queryOrders() {
            sales.queryOrders()
        }

        override fun getReceipts(type: Product.Type?): LiveData<List<Receipt>> {
            if(client is GoogleClient) {
                val skuType = if(type == Product.Type.SUBSCRIPTION) BillingClient.SkuType.SUBS else BillingClient.SkuType.INAPP
                client.getBillingClient()?.queryPurchaseHistoryAsync(skuType, sales)
                sales.queryReceipts(type)
            }
            return sales.orderHistory
        }

        override fun updateInventory(skuList: List<String>, type: Product.Type) {
            inventory.queryInventory(skuList = skuList, type)
        }

        override fun getProducts(type: Product.Type?, promo: Product.Promotion?): List<Product> {
            return inventory.getAvailableProducts(type, promo)
        }

        override fun getProduct(sku: String): Product? {
            return inventory.getProduct(sku)
        }
    }

    /*****************************************************************************************************
     * Public Methods - Facade Pattern
     *****************************************************************************************************/

    /**
     * Returns the primary class for developers to conveniently
     * interact with Android's Billing Library (Facade pattern).
     * @return [Agent]
     */
    override fun getAgent(): Agent {
        return storeAgent
    }

    companion object {
        private const val TAG = "GoogleStore"
    }

    class Builder {

        private val store = GoogleStore()

        fun create(context: Context?): Builder {
            return this
        }

        fun setOrderUpdateListener(listener: Sales.OrderUpdaterListener): Builder {
            store.sales.orderUpdaterListener = listener
            return this
        }

        fun setOrderResumeListener(listener: Sales.OrderValidatorListener): Builder {
            store.sales.orderValidatorListener = listener
            return this
        }

        fun build(): GoogleStore {
            return GoogleStore()
        }
    }
}
