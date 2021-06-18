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
import androidx.collection.ArrayMap
import androidx.lifecycle.LiveData
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchaseHistoryResponseListener
import com.android.billingclient.api.SkuDetails
import com.zuko.billingz.lib.LogUtil
import com.zuko.billingz.lib.store.StoreLifecycle
import com.zuko.billingz.lib.store.agent.Agent
import com.zuko.billingz.lib.store.inventory.Inventory
import com.zuko.billingz.google.store.products.Consumable
import com.zuko.billingz.google.store.products.NonConsumable
import com.zuko.billingz.lib.store.products.Product
import com.zuko.billingz.google.store.products.Subscription
import com.zuko.billingz.lib.store.sales.Sales
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel

/**
 * @author rjsuzuki
 * //TODO handle pending purchases?
 * //TODO retry connection
 */
class GoogleStore : StoreLifecycle {

    private val mainScope = MainScope()

    private val billing: com.zuko.billingz.lib.store.client.Client = Client()
    private val inventory: Inventory = ProductInventory(billing)
    private val sales: Sales = ProductSales(inventory)
    private val history: History = OrderHistory(billing)
    private var isInitialized = false

    private val googlePlayConnectListener = object : com.zuko.billingz.lib.store.client.Billing.Client.GooglePlayConnectListener {
        override fun connected() {
            history.refreshOrderHistory(sales)
        }
    }
    /*****************************************************************************************************
     * Lifecycle events - developer must either add this class to a lifecycleOwner or manually add the events
     * to their respective parent view
     *****************************************************************************************************/
    init {
        LogUtil.log.v(TAG, "instantiating...")
    }

    override fun init(context: Context?) {
        LogUtil.log.v(TAG, "initializing...")
        billing.initClient(context, sales.purchasesUpdatedListener, googlePlayConnectListener)
        isInitialized = true
    }

    override fun create() {
        LogUtil.log.v(TAG, "creating...")
        setOrderUpdateListener()
        if (isInitialized && billing.initialized()) {
            billing.connect()
        }
        history.refreshOrderHistory(sales) // might need to react to connection
    }

    override fun start() {
        LogUtil.log.v(TAG, "starting...")
    }

    override fun resume() {
        LogUtil.log.v(TAG, "resuming...")
        billing.checkConnection()
        history.refreshOrderHistory(sales)
    }

    override fun pause() {
        LogUtil.log.v(TAG, "pausing...")
    }

    override fun stop() {
        LogUtil.log.v(TAG, "stopping...")
        billing.disconnect()
    }

    override fun destroy() {
        LogUtil.log.v(TAG, "destroying...")
        billing.destroy()
        history.destroy()
        sales.destroy()
        inventory.destroy()
        mainScope.cancel()
    }

    /*****************************************************************************************************
     * Private methods
     *****************************************************************************************************/
    private fun setOrderUpdateListener() {
        sales.orderUpdateListener = object : Sales.OrderUpdateListener {
            override fun resumeOrder(purchase: Purchase, productType: Product.Type) {
                LogUtil.log.i(TAG, "Attempting to complete purchase order : $purchase, type: $productType")
                when (productType) {
                    Product.Type.SUBSCRIPTION -> {
                        Subscription.completeOrder(billing.getBillingClient(), purchase, sales.getOrderOrQueried(), mainScope = mainScope)
                    }

                    Product.Type.NON_CONSUMABLE -> {
                        NonConsumable.completeOrder(billing.getBillingClient(), purchase, sales.getOrderOrQueried(), mainScope = mainScope)
                    }

                    Product.Type.CONSUMABLE -> {
                        Consumable.completeOrder(billing.getBillingClient(), purchase, sales.getOrderOrQueried(), null)
                    }
                    else -> LogUtil.log.v(TAG, "Unhandled product type: $productType")
                }
            }
        }
    }

    private val billingAgent = object : Agent {

        override fun isBillingClientReady(): LiveData<Boolean> {
            return billing.isClientReady
        }

        override fun queriedOrders(listener: Sales.OrderValidatorListener): LiveData<Order> {
            sales.orderValidatorListener = listener
            return sales.queriedOrder
        }

        override fun startOrder(
            activity: Activity?,
            productId: String?,
            listener: Sales.OrderValidatorListener?
        ): LiveData<Order> {
            LogUtil.log.v(TAG, "Starting purchase flow")
            sales.orderValidatorListener = listener

            val skuDetails = inventory.allProducts[productId]

            activity?.let { a ->
                skuDetails?.let {
                    billing.getBillingClient()?.let { client ->
                        val result = sales.startPurchaseRequest(a, skuDetails, client)
                        val order = Order(
                            billingResult = result,
                            msg = "Processing..."
                        )
                        sales.order.postValue(order)
                    }
                }
            }
            return sales.order
        }

        override fun getAvailableProducts(
            skuList: MutableList<String>,
            productType: Product.Type
        ): LiveData<Map<String, SkuDetails>> {
            LogUtil.log.v(TAG, "addProductsToInventory")
            when (productType) {
                Product.Type.NON_CONSUMABLE -> inventory.loadInAppProducts(skuList, false)
                Product.Type.CONSUMABLE -> inventory.loadInAppProducts(skuList, true)
                Product.Type.SUBSCRIPTION -> inventory.loadSubscriptions(skuList)
                Product.Type.FREE_CONSUMABLE -> inventory.loadFreeProducts(skuList, productType)
                Product.Type.FREE_NON_CONSUMABLE -> inventory.loadFreeProducts(skuList, productType)
                Product.Type.FREE_SUBSCRIPTION -> inventory.loadFreeProducts(skuList, productType)
                Product.Type.PROMO_CONSUMABLE -> inventory.loadPromotions(skuList, productType)
                Product.Type.PROMO_NON_CONSUMABLE -> inventory.loadPromotions(skuList, productType)
                Product.Type.PROMO_SUBSCRIPTION -> inventory.loadPromotions(skuList, productType)
                else -> LogUtil.log.w(TAG, "Unhandled product type: $productType")
            }
            return inventory.requestedProducts
        }

        override fun getProductDetails(productId: String): SkuDetails? {
            return inventory.getProductDetails(productId)
        }

        override fun getReceipts(skuType: String, listener: PurchaseHistoryResponseListener) {
            billing.getBillingClient()?.queryPurchaseHistoryAsync(skuType, listener)
        }

        override fun getPendingOrders(): ArrayMap<String, Purchase> {
            return sales.pendingPurchases
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
    @Suppress("unused")
    fun getAgent(): Agent {
        return billingAgent
    }

    companion object {
        private const val TAG = "GoogleStore"
    }
}
