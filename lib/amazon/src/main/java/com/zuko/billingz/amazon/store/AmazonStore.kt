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
package com.zuko.billingz.amazon.store

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.collection.ArrayMap
import androidx.lifecycle.LiveData
import com.zuko.billingz.amazon.store.client.AmazonClient
import com.zuko.billingz.amazon.store.inventory.AmazonInventory
import com.zuko.billingz.amazon.store.model.AmazonOrder
import com.zuko.billingz.amazon.store.sales.AmazonSales
import com.zuko.billingz.core.LogUtilz
import com.zuko.billingz.core.store.Storez
import com.zuko.billingz.core.store.agent.Agentz
import com.zuko.billingz.core.store.client.Clientz
import com.zuko.billingz.core.store.model.OrderHistoryz
import com.zuko.billingz.core.store.model.Orderz
import com.zuko.billingz.core.store.model.Productz
import com.zuko.billingz.core.store.model.QueryResult
import com.zuko.billingz.core.store.sales.Salez
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow

/**
 * @author rjsuzuki
 */
@Suppress("unused")
class AmazonStore internal constructor() : Storez {

    private val connectionListener = object : Clientz.ConnectionListener {
        override fun connected() {
            sales.refreshQueries()
        }
    }
    private val mainScope by lazy { MainScope() }
    private var context: Context? = null
    private val inventory = AmazonInventory()
    private val sales = AmazonSales(inventory)
    private val client = AmazonClient(inventory, sales)

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
        client.pause()
    }

    override fun stop() {
        LogUtilz.log.v(TAG, "stopping...")
    }

    override fun destroy() {
        LogUtilz.log.v(TAG, "destroying...")
        mainScope.cancel()
        inventory.destroy()
        client.destroy()
    }

    private val storeAgent = object : Agentz {

        override fun isInventoryReadyLiveData(): LiveData<Boolean> {
            return inventory.isReadyLiveData()
        }

        override fun isInventoryReadyStateFlow(): StateFlow<Boolean> {
            return inventory.isReadyStateFlow()
        }

        override fun getState(): LiveData<Clientz.ConnectionStatus> {
            LogUtilz.log.v(TAG, "isBillingClientReady")
            return client.connectionState
        }

        override fun startOrder(
            activity: Activity?,
            productId: String?,
            options: Bundle?
        ): LiveData<Orderz> {
            LogUtilz.log.v(TAG, "Starting order: $productId")

            val product = inventory.getProduct(productId)
            if (product == null) {
                val order = AmazonOrder(
                    resultMessage = "No matching sku found in inventory.",
                    result = Orderz.Result.INVALID_PRODUCT,
                    null,
                    null,
                    null,
                    null,
                    null
                )
                sales.currentOrder.postValue(order)
            } else {
                sales.startOrder(activity, product, client)
            }
            return sales.currentOrder
        }

        override fun queryOrders(): QueryResult<Orderz> {
            LogUtilz.log.v(TAG, "queryOrders")
            return sales.queryOrders()
        }

        override fun queryReceipts(type: Productz.Type?): QueryResult<OrderHistoryz> {
            LogUtilz.log.v(TAG, "getReceipts: $type")
            return sales.queryReceipts(type)
        }

        override fun queryInventory(products: Map<String, Productz.Type>): QueryResult<Map<String, Productz>> {
            LogUtilz.log.v(TAG, "updateInventory: ${products.size}")
            return inventory.queryInventory(products = products)
        }

        override fun queryProduct(sku: String, type: Productz.Type): QueryResult<Productz> {
            return inventory.queryProduct(sku, type)
        }

        override fun getProducts(
            type: Productz.Type?,
            promo: Productz.Promotion?
        ): Map<String, Productz> {
            LogUtilz.log.v(TAG, "getProducts: $type : $promo")
            return inventory.getProducts(
                type = type,
                promo = promo
            )
        }

        override fun completeOrder(order: Orderz) {
            sales.completeOrder(order)
        }

        override fun cancelOrder(order: Orderz) {
            sales.cancelOrder(order)
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
     * Builder Pattern - create an instance of AmazonStore
     */
    @Suppress("unused")
    class Builder : Storez.Builder {
        private lateinit var instance: AmazonStore
        private lateinit var updaterListener: Salez.OrderUpdaterListener
        private lateinit var validatorListener: Salez.OrderValidatorListener
        private lateinit var products: ArrayMap<String, Productz.Type>
        private var accountId: String? = null
        private var isNewVersion = false
        /**
         * @param listener - Required to be set for proper functionality
         */
        override fun setOrderUpdater(listener: Salez.OrderUpdaterListener): Builder {
            updaterListener = listener
            return this
        }

        /**
         * @param listener - Required to be set for proper functionality
         */
        override fun setOrderValidator(listener: Salez.OrderValidatorListener): Builder {
            validatorListener = listener
            return this
        }

        /**
         * Not used in this implementation
         */
        override fun setAccountId(id: String?): Builder {
            accountId = id
            return this
        }

        override fun setNewVersion(enable: Boolean): Storez.Builder {
            isNewVersion = enable
            return this
        }

        override fun build(context: Context?): Storez {
            instance = AmazonStore()
            instance.sales.apply {
                orderUpdaterListener = updaterListener
                orderValidatorListener = validatorListener
            }
            instance.inventory.isNewVersion = isNewVersion
            instance.sales.isNewVersion = isNewVersion
            instance.init(context = context)
            instance.create()
            return instance
        }
    }

    companion object {
        private const val TAG = "AmazonStore"
    }
}
