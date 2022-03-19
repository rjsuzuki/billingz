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
package com.zuko.billingz.amazon.store

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.collection.ArrayMap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.zuko.billingz.amazon.store.client.AmazonClient
import com.zuko.billingz.amazon.store.inventory.AmazonInventory
import com.zuko.billingz.amazon.store.sales.AmazonSales
import com.zuko.billingz.core.LogUtilz
import com.zuko.billingz.core.store.Storez
import com.zuko.billingz.core.store.agent.Agentz
import com.zuko.billingz.core.store.client.Clientz
import com.zuko.billingz.core.store.model.Orderz
import com.zuko.billingz.core.store.model.Productz
import com.zuko.billingz.core.store.model.Receiptz
import com.zuko.billingz.core.store.sales.Salez
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow

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
    private val sales = AmazonSales()
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
    }

    override fun stop() {
        LogUtilz.log.v(TAG, "stopping...")
        mainScope.cancel()
    }

    override fun destroy() {
        LogUtilz.log.v(TAG, "destroying...")
        mainScope.cancel()
        inventory.destroy()
        client.destroy()
    }

    private val agent = object : Agentz {

        override fun isInventoryReady(): LiveData<Boolean> {
            val data = MutableLiveData<Boolean>()
            data.value =
                client.isReady() && inventory.requestedProducts.value?.isNullOrEmpty() == false
            return data
        }

        override fun getState(): LiveData<Clientz.ConnectionStatus> {
            LogUtilz.log.v(TAG, "isBillingClientReady")
            return client.connectionState
        }

        override fun startOrder(
            activity: Activity?,
            productId: String?,
            options: Bundle?,
            listener: Salez.OrderValidatorListener?
        ): LiveData<Orderz> {
            LogUtilz.log.v(TAG, "Starting order: $productId")
            val data = MutableLiveData<Orderz>()
            val product = inventory.getProduct(productId)
            product?.let {
                sales.startOrder(activity, product, client)
            }
            return data
        }

        override fun queryOrders(): LiveData<Orderz> {
            LogUtilz.log.v(TAG, "queryOrders")
            return sales.queryOrders()
        }

        override suspend fun queryProduct(sku: String, type: Productz.Type): Productz? {
            return inventory.queryProduct(sku, type)
        }

        override fun queryProductFlow(sku: String, type: Productz.Type): Flow<Productz> {
            return inventory.queryProductFlow(sku, type)
        }

        override fun queryReceipts(type: Productz.Type?): LiveData<ArrayMap<String, Receiptz>> {
            LogUtilz.log.v(TAG, "getReceipts: $type")
            return sales.orderHistory
        }

        override fun updateInventory(products: Map<String, Productz.Type>): LiveData<Map<String, Productz>> {
            LogUtilz.log.v(TAG, "updateInventory: ${products.size}")
            return inventory.queryInventory(products = products)
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

        override fun getProduct(sku: String?): Productz? {
            LogUtilz.log.v(TAG, "getProduct: $sku")
            return inventory.getProduct(sku = sku)
        }
    }

    override fun getAgent(): Agentz {
        return agent
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

        override fun setAccountId(id: String?): Storez.Builder {
            accountId = id
            return this
        }

        override fun setProducts(products: ArrayMap<String, Productz.Type>): Builder {
            this.products = products
            return this
        }

        override fun build(context: Context?): AmazonStore {
            instance = AmazonStore()
            instance.sales.apply {
                orderUpdaterListener = updaterListener
                orderValidatorListener = validatorListener
            }
            instance.init(context = context)
            instance.client.connect()
            if (::products.isInitialized) {
                instance.inventory.queryInventory(products = this.products)
            }
            return instance
        }
    }

    companion object {
        private const val TAG = "AmazonStore"
    }
}
