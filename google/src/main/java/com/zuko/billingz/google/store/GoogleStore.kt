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
import androidx.lifecycle.MutableLiveData
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PurchasesUpdatedListener
import com.zuko.billingz.google.store.client.GoogleClient
import com.zuko.billingz.google.store.inventory.GoogleInventory
import com.zuko.billingz.google.store.model.GoogleOrder
import com.zuko.billingz.google.store.sales.GoogleSales
import com.zuko.billingz.core.LogUtilz
import com.zuko.billingz.core.store.Storez
import com.zuko.billingz.core.store.agent.Agentz
import com.zuko.billingz.core.store.client.Clientz
import com.zuko.billingz.core.store.inventory.Inventoryz
import com.zuko.billingz.core.store.model.Orderz
import com.zuko.billingz.core.store.model.Productz
import com.zuko.billingz.core.store.model.Receiptz
import com.zuko.billingz.core.store.sales.Salez
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel

/**
 * @author rjsuzuki
 * //TODO handle pending purchases?
 * //TODO retry connection
 */
class GoogleStore: Storez {

    private val mainScope = MainScope()
    private val purchasesUpdatedListener: PurchasesUpdatedListener =
        PurchasesUpdatedListener { billingResult, purchases -> (sales as GoogleSales).processUpdatedPurchases(billingResult, purchases) }
    private val connectionListener = object : Clientz.ConnectionListener {
        override fun connected() {
            sales.refreshQueries()
        }
    }

    private val client: Clientz = GoogleClient(purchasesUpdatedListener)
    private val inventory: Inventoryz = GoogleInventory(client as GoogleClient)
    private val sales: Salez = GoogleSales(inventory as GoogleInventory, client as GoogleClient)

    private var isInitialized = false


    /*****************************************************************************************************
     * Lifecycle events - developer must either add this class to a lifecycleOwner or manually add the events
     * to their respective parent view
     *****************************************************************************************************/
    init {
        LogUtilz.log.v(TAG, "instantiating...")
    }

    override fun init(context: Context?) {
        LogUtilz.log.v(TAG, "initializing...")
        client.init(context, connectionListener)
        isInitialized = true
    }

    override fun create() {
        LogUtilz.log.v(TAG, "creating...")
        if (isInitialized && client.initialized())
            client.connect()

        if(client.isReady())
            sales.refreshQueries() // might need to react to connection
    }

    override fun start() {
        LogUtilz.log.v(TAG, "starting...")
    }

    override fun resume() {
        LogUtilz.log.v(TAG, "resuming...")
        client.checkConnection()
        if(client.isReady())
            sales.refreshQueries()
    }

    override fun pause() {
        LogUtilz.log.v(TAG, "pausing...")
    }

    override fun stop() {
        LogUtilz.log.v(TAG, "stopping...")
        client.disconnect()
    }

    override fun destroy() {
        LogUtilz.log.v(TAG, "destroying...")
        client.destroy()
        sales.destroy()
        inventory.destroy()
        mainScope.cancel()
    }

    /*****************************************************************************************************
     * Private methods
     *****************************************************************************************************/

    private val storeAgent = object : Agentz {

        override fun isBillingClientReady(): LiveData<Boolean> {
            return client.isClientReady
        }

        override fun startOrder(
            activity: Activity?,
            productId: String?,
            listener: Salez.OrderValidatorListener?
        ): LiveData<Orderz> {
            LogUtilz.log.v(TAG, "Starting purchase flow")

            sales.orderValidatorListener = listener

            val data = MutableLiveData<Orderz>()
            val product = inventory.allProducts[productId]
            product?.let {
                sales.startOrder(activity, product, client)
                val order = GoogleOrder(
                    billingResult = null,
                    msg = "Processing..."
                )
                data.postValue(order)
            } ?: data.postValue(GoogleOrder(
                billingResult = BillingResult.newBuilder()
                    .setDebugMessage("Product: $productId not found.")
                    .setResponseCode(BillingClient.BillingResponseCode.ITEM_UNAVAILABLE)
                    .build(),
                msg = "Product: $productId not found."
            ))
            return data
        }

        override fun queryOrders() {
            sales.queryOrders()
        }

        override fun getReceipts(type: Productz.Type?): LiveData<List<Receiptz>> {
            if(client is GoogleClient) {
                val skuType = if(type == Productz.Type.SUBSCRIPTION) BillingClient.SkuType.SUBS else BillingClient.SkuType.INAPP
                //todo client.getBillingClient()?.queryPurchaseHistoryAsync(skuType, sales)
                sales.queryReceipts(type)
            }
            return sales.orderHistory
        }

        override fun updateInventory(skuList: List<String>, type: Productz.Type) {
            inventory.queryInventory(skuList = skuList, type)
        }

        override fun getProducts(type: Productz.Type?, promo: Productz.Promotion?): List<Productz> {
            return inventory.getProducts(type, promo)
        }

        override fun getProduct(sku: String): Productz? {
            return inventory.getProduct(sku)
        }
    }

    /*****************************************************************************************************
     * Public Methods - Facade Pattern
     *****************************************************************************************************/

    /**
     * Returns the primary class for developers to conveniently
     * interact with Android's Billing Library (Facade pattern).
     * @return [Agentz]
     */
    override fun getAgent(): Agentz {
        return storeAgent
    }

    companion object {
        private const val TAG = "GoogleStore"
    }
}
