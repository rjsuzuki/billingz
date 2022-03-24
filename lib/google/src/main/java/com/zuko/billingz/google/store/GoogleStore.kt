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
package com.zuko.billingz.google.store

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.collection.ArrayMap
import androidx.lifecycle.LiveData
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PurchasesUpdatedListener
import com.zuko.billingz.core.LogUtilz
import com.zuko.billingz.core.store.Storez
import com.zuko.billingz.core.store.agent.Agentz
import com.zuko.billingz.core.store.client.Clientz
import com.zuko.billingz.core.store.inventory.Inventoryz
import com.zuko.billingz.core.store.model.OrderHistoryz
import com.zuko.billingz.core.store.model.Orderz
import com.zuko.billingz.core.store.model.Productz
import com.zuko.billingz.core.store.model.QueryResult
import com.zuko.billingz.core.store.sales.Salez
import com.zuko.billingz.core.store.security.Securityz
import com.zuko.billingz.google.store.client.GoogleClient
import com.zuko.billingz.google.store.inventory.GoogleInventory
import com.zuko.billingz.google.store.model.GoogleOrder
import com.zuko.billingz.google.store.sales.GoogleSales
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow

/**
 * @author rjsuzuki
 */
@Suppress("unused")
class GoogleStore internal constructor() : Storez {

    private val purchasesUpdatedListener: PurchasesUpdatedListener =
        PurchasesUpdatedListener { billingResult, purchases ->
            (sales as GoogleSales).processUpdatedPurchases(
                billingResult,
                purchases
            )
        }
    private val connectionListener = object : Clientz.ConnectionListener {
        override fun connected() {
            sales.refreshQueries()
        }
    }
    private val mainScope by lazy { MainScope() }
    private var context: Context? = null
    private val client: Clientz = GoogleClient(purchasesUpdatedListener)
    private val inventory: Inventoryz = GoogleInventory(client as GoogleClient)
    private val sales: Salez = GoogleSales(inventory as GoogleInventory, client as GoogleClient)

    init {
        LogUtilz.log.v(TAG, "instantiating...")
    }

    override fun init(context: Context?) {
        LogUtilz.log.v(TAG, "initializing...")
        this.context = context
    }

    override fun create() {
        LogUtilz.log.v(TAG, "creating...")
        if (!client.initialized()) {
            client.init(context, connectionListener)
            client.connect()
        }
        client.checkConnection()
    }

    override fun start() {
        LogUtilz.log.v(TAG, "starting...")
    }

    override fun resume() {
        LogUtilz.log.v(TAG, "resuming...")
        if (client.isReady())
            sales.refreshQueries()
        else if (!client.initialized()) {
            client.init(context, connectionListener)
            client.connect()
        } else {
            client.checkConnection()
        }
    }

    override fun pause() {
        LogUtilz.log.v(TAG, "pausing...")
    }

    override fun stop() {
        LogUtilz.log.v(TAG, "stopping...")
        mainScope.cancel()
    }

    override fun destroy() {
        LogUtilz.log.v(TAG, "destroying...")
        client.destroy()
        sales.destroy()
        inventory.destroy()
    }

    private val storeAgent = object : Agentz {

        override fun isInventoryReadyLiveData(): LiveData<Boolean> {
            return inventory.isReadyLiveData()
        }

        override fun isInventoryReadyStateFlow(): StateFlow<Boolean> {
            return inventory.isReadyStateFlow()
        }

        override fun getState(): LiveData<Clientz.ConnectionStatus> {
            LogUtilz.log.v(TAG, "isBillingClientReady: ${client.isReady()}")
            return client.connectionState
        }

        override fun startOrder(
            activity: Activity?,
            productId: String?,
            options: Bundle?,
            listener: Salez.OrderValidatorListener?
        ): LiveData<Orderz> {
            LogUtilz.log.v(TAG, "Starting order: $productId")
            sales.orderValidatorListener = listener

            val product = inventory.getProduct(productId)
            product?.let {
                sales.startOrder(activity, product, client)
            } ?: run {
                sales.currentOrder.postValue(
                    GoogleOrder(
                        purchase = null,
                        billingResult = BillingResult.newBuilder()
                            .setDebugMessage("Product: $productId not found.")
                            .setResponseCode(BillingClient.BillingResponseCode.ITEM_UNAVAILABLE)
                            .build()
                    )
                )
            }
            return sales.currentOrder
        }

        override fun queryOrders(): QueryResult<Orderz> {
            LogUtilz.log.v(TAG, "queryOrders")
            return sales.queryOrders()
        }

        override fun queryProduct(sku: String, type: Productz.Type): QueryResult<Productz> {
            LogUtilz.log.v(TAG, "queryProduct: \nsku: $sku,\ntype: $type")
            return inventory.queryProduct(sku, type)
        }

        override fun queryReceipts(type: Productz.Type?): QueryResult<OrderHistoryz> {
            LogUtilz.log.v(TAG, "queryReceipts:\ntype: $type")
            return sales.queryReceipts(type)
        }

        override fun queryInventory(products: Map<String, Productz.Type>): QueryResult<Map<String, Productz>> {
            LogUtilz.log.v(TAG, "queryInventory:\n products: ${products.size}")
            return inventory.queryInventory(products = products)
        }

        override fun getProducts(
            type: Productz.Type?,
            promo: Productz.Promotion?
        ): Map<String, Productz> {
            LogUtilz.log.v(TAG, "getProducts: $type : $promo")
            return inventory.getProducts(type = type, promo = promo)
        }

        override fun getProduct(sku: String?): Productz? {
            LogUtilz.log.v(TAG, "getProduct: $sku")
            return inventory.getProduct(sku = sku)
        }
    }

    override fun getAgent(): Agentz {
        return storeAgent
    }

    /**
     * Builder Pattern - create an instance of GoogleStore
     */
    @Suppress("unused")
    class Builder : Storez.Builder {
        private lateinit var instance: GoogleStore
        private lateinit var updaterListener: Salez.OrderUpdaterListener
        private lateinit var validatorListener: Salez.OrderValidatorListener
        private var obfuscatedAccountId: String? = null
        private var obfuscatedProfileId: String? = null
        private var hashingSalt: String? = null
        private lateinit var products: ArrayMap<String, Productz.Type>

        override fun setOrderUpdater(listener: Salez.OrderUpdaterListener): Builder {
            updaterListener = listener
            return this
        }

        override fun setOrderValidator(listener: Salez.OrderValidatorListener): Builder {
            validatorListener = listener
            return this
        }

        /**
         * Specify a salt to use when obfuscating account id or profile id
         * @param - a string to use as salt for the hashing of identifiers
         */
        fun setObfuscatingHashingSalt(salt: String?) {
            hashingSalt = salt
        }

        override fun setAccountId(id: String?): Builder {
            if (!id.isNullOrBlank()) {
                obfuscatedAccountId = Securityz.sha256(id, hashingSalt)
            }
            return this
        }

        /**
         * Some applications allow users to have multiple profiles within a single account.
         * Use this method to send the user's profile identifier to Google.
         * @param - unique identifier for the user's profile (64 character limit).
         * The profile ID is obfuscated using SHA-256 before being cached and used.
         */
        fun setProfileId(id: String?): Builder {
            if (!id.isNullOrBlank()) {
                obfuscatedProfileId = Securityz.sha256(id, hashingSalt)
            }
            return this
        }

        override fun setProducts(products: ArrayMap<String, Productz.Type>): Builder {
            this.products = products
            return this
        }

        override fun build(context: Context?): Storez {
            instance = GoogleStore()
            instance.sales.apply {
                orderUpdaterListener = updaterListener
                orderValidatorListener = validatorListener
            }
            instance.sales.setObfuscatedIdentifiers(
                accountId = obfuscatedAccountId,
                profileId = obfuscatedProfileId
            )

            instance.init(context = context)
            instance.client.connect()
            if (::products.isInitialized) {
                instance.inventory.queryInventory(products = this.products)
            }
            return instance
        }
    }

    companion object {
        private const val TAG = "   GoogleStore"
    }
}
