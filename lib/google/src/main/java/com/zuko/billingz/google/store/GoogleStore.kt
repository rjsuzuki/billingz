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
import androidx.lifecycle.LiveData
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PurchasesUpdatedListener
import com.zuko.billingz.core.misc.Logger
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
import com.zuko.billingz.google.BuildConfig
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
        Logger.v(TAG, "instantiating...")
    }

    override fun init(context: Context?) {
        Logger.i(
            TAG,
            "Initializing..." +
                "\n debug: ${BuildConfig.DEBUG}" +
                "\n build: ${BuildConfig.BUILD_TYPE}" +
                "\n version: ${BuildConfig.VERSION}"
        )
        this.context = context
    }

    override fun create() {
        Logger.v(TAG, "creating...")
        if (!client.initialized()) {
            client.init(context, connectionListener)
            client.connect()
        }
        client.checkConnection()
    }

    override fun start() {
        Logger.v(TAG, "starting...")
    }

    override fun resume() {
        Logger.v(TAG, "resuming...")
        if (client.isReady()) {
            sales.refreshQueries()
        } else if (!client.initialized()) {
            client.init(context, connectionListener)
            client.connect()
        } else {
            client.checkConnection()
        }
    }

    override fun pause() {
        Logger.v(TAG, "pausing...")
    }

    override fun stop() {
        Logger.v(TAG, "stopping...")
    }

    override fun destroy() {
        Logger.v(TAG, "destroying...")
        mainScope.cancel()
        client.destroy()
        sales.destroy()
        inventory.destroy()
    }

    @Suppress("OverridingDeprecatedMember")
    private val storeAgent = object : Agentz {

        override fun updateIdentifiers(accountId: String?, profileId: String?, hashingSalt: String?) {
            var hashedAccountId: String? = null
            var hashedProfileId: String? = null
            if (!accountId.isNullOrBlank()) {
                hashedAccountId = Securityz.sha256(accountId, hashingSalt)
            }
            if (!profileId.isNullOrBlank()) {
                hashedProfileId = Securityz.sha256(profileId, hashingSalt)
            }
            sales.setObfuscatedIdentifiers(
                accountId = hashedAccountId,
                profileId = hashedProfileId
            )
        }

        override fun isInventoryReadyLiveData(): LiveData<Boolean> {
            return inventory.isReadyLiveData()
        }

        override fun isInventoryReadyStateFlow(): StateFlow<Boolean> {
            return inventory.isReadyStateFlow()
        }

        override fun getState(): LiveData<Clientz.ConnectionStatus> {
            Logger.v(TAG, "isBillingClientReady: ${client.isReady()}")
            return client.connectionState
        }

        override fun startOrder(
            activity: Activity?,
            productId: String?,
            options: Bundle?
        ): LiveData<Orderz> {
            Logger.v(TAG, "Starting order: $productId")

            val product = inventory.getProduct(productId)
            product?.let {
                sales.startOrder(
                    activity = activity,
                    product = product,
                    client = client,
                    options = options
                )
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
            Logger.v(TAG, "queryOrders")
            return sales.queryOrders()
        }

        override fun queryProduct(sku: String, type: Productz.Type): QueryResult<Productz> {
            Logger.v(TAG, "queryProduct: \nsku: $sku,\ntype: $type")
            return inventory.queryProduct(sku, type)
        }

        override fun queryReceipts(type: Productz.Type?): QueryResult<OrderHistoryz> {
            Logger.v(TAG, "queryReceipts:\ntype: $type")
            return sales.queryReceipts(type)
        }

        override fun queryInventory(products: Map<String, Productz.Type>): QueryResult<Map<String, Productz>> {
            Logger.v(TAG, "queryInventory:\n products: ${products.size}")
            return inventory.queryInventory(products = products)
        }

        @Deprecated("Will be removed in a future release")
        override fun getProducts(
            type: Productz.Type?,
            promo: Productz.Promotion?
        ): Map<String, Productz> {
            Logger.v(TAG, "getProducts: $type : $promo")
            return inventory.getProducts(type = type, promo = promo)
        }

        override fun completeOrder(order: Orderz) {
            sales.completeOrder(order)
        }

        override fun cancelOrder(order: Orderz) {
            sales.cancelOrder(order)
        }

        @Deprecated("Will be removed in a future release.")
        override fun getProduct(sku: String?): Productz? {
            Logger.v(TAG, "getProduct: $sku")
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
        private var isNewVersion = false

        override fun setOrderUpdater(listener: Salez.OrderUpdaterListener): Builder {
            updaterListener = listener
            return this
        }

        override fun setOrderValidator(listener: Salez.OrderValidatorListener): Builder {
            validatorListener = listener
            return this
        }

        override fun setObfuscatingHashingSalt(salt: String?): Builder {
            hashingSalt = salt
            return this
        }

        override fun setAccountId(id: String?): Builder {
            if (!id.isNullOrBlank()) {
                obfuscatedAccountId = Securityz.sha256(id, hashingSalt)
            }
            return this
        }

        override fun setProfileId(id: String?): Builder {
            if (!id.isNullOrBlank()) {
                obfuscatedProfileId = Securityz.sha256(id, hashingSalt)
            }
            return this
        }

        override fun setNewVersion(enable: Boolean): Storez.Builder {
            isNewVersion = enable
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
            instance.sales.isNewVersion = isNewVersion
            instance.inventory.isNewVersion = isNewVersion
            instance.init(context = context)
            instance.client.connect()
            return instance
        }
    }

    companion object {
        private const val TAG = "GoogleStore"
    }
}
