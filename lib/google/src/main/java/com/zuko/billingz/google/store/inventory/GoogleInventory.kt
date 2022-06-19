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
package com.zuko.billingz.google.store.inventory

import androidx.collection.ArrayMap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.SkuDetails
import com.android.billingclient.api.SkuDetailsParams
import com.zuko.billingz.core.LogUtilz
import com.zuko.billingz.core.misc.BillingzDispatcher
import com.zuko.billingz.core.misc.Dispatcherz
import com.zuko.billingz.core.store.inventory.Inventoryz
import com.zuko.billingz.core.store.model.Productz
import com.zuko.billingz.core.store.model.QueryResult
import com.zuko.billingz.google.store.client.GoogleClient
import com.zuko.billingz.google.store.model.GoogleInventoryQuery
import com.zuko.billingz.google.store.model.GoogleProduct
import com.zuko.billingz.google.store.model.GoogleProductQuery
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GoogleInventory(
    private val client: GoogleClient,
    private val dispatcher: Dispatcherz = BillingzDispatcher()
) : Inventoryz {

    override var allProducts: Map<String, Productz.Type> = ArrayMap()

    override var consumables: ArrayMap<String, Productz> = ArrayMap()
    override var nonConsumables: ArrayMap<String, Productz> = ArrayMap()
    override var subscriptions: ArrayMap<String, Productz> = ArrayMap()

    private val isReadyLiveData = MutableLiveData<Boolean>()
    private val isReadyStateFlow = MutableStateFlow(false)
    private val isReadyState = isReadyStateFlow.asStateFlow()

    private val requestedProductsLiveData: MutableLiveData<ArrayMap<String, Productz>> =
        MutableLiveData()
    private val requestedProductsStateFlow: MutableStateFlow<ArrayMap<String, Productz>> by lazy {
        MutableStateFlow(
            ArrayMap()
        )
    }
    private val requestedProductsState: StateFlow<ArrayMap<String, Productz>> by lazy { requestedProductsStateFlow.asStateFlow() }

    private val queriedProductLiveData = MutableLiveData<Productz>()
    private val queriedProductStateFlow: MutableStateFlow<Productz?> = MutableStateFlow(null)
    private val queriedProductState = queriedProductStateFlow.asStateFlow()

    private val mainScope = MainScope()

    private fun queryProducts(skus: List<String>, type: Productz.Type) {
        val skuType = when (type) {
            Productz.Type.CONSUMABLE -> BillingClient.SkuType.INAPP
            Productz.Type.NON_CONSUMABLE -> BillingClient.SkuType.INAPP
            Productz.Type.SUBSCRIPTION -> BillingClient.SkuType.SUBS
            else -> {
                BillingClient.SkuType.INAPP
            }
        }
        val builder = SkuDetailsParams.newBuilder()
        val params = builder
            .setSkusList(skus)
            .setType(skuType)
            .build()
        client.getBillingClient()?.querySkuDetailsAsync(params) { billingResult, skuDetailsList ->
            handleQueryResult(
                result = billingResult,
                skuDetailsList = skuDetailsList,
                type = type
            )
        }
    }

    private fun handleQueryResult(
        result: BillingResult?,
        skuDetailsList: List<SkuDetails>?,
        type: Productz.Type
    ) {
        LogUtilz.log.d(
            TAG,
            "Processing inventory query result ->" +
                "\n type: $type," +
                "\n billingResult code: ${result?.responseCode}," +
                "\n billingResult msg: ${result?.debugMessage ?: "n/a"}," +
                "\n products: $skuDetailsList" +
                "\n -----------------------------------"
        )
        if (result?.responseCode == BillingClient.BillingResponseCode.OK &&
            !skuDetailsList.isNullOrEmpty()
        ) {
            val availableProducts = mutableListOf<Productz>()
            skuDetailsList.let { skus ->
                for (s in skus) {
                    val product = GoogleProduct(skuDetails = s, type = type)
                    availableProducts.add(product)
                }
            }
            updateInventory(products = availableProducts, type = type)
        }
    }

    override fun queryProduct(sku: String, type: Productz.Type): QueryResult<Productz> {
        val skuType = when (type) {
            Productz.Type.CONSUMABLE -> BillingClient.SkuType.INAPP
            Productz.Type.NON_CONSUMABLE -> BillingClient.SkuType.INAPP
            Productz.Type.SUBSCRIPTION -> BillingClient.SkuType.SUBS
            else -> {
                BillingClient.SkuType.INAPP
            }
        }
        val builder = SkuDetailsParams.newBuilder()
        val params = builder
            .setSkusList(listOf(sku))
            .setType(skuType)
            .build()
        client.getBillingClient()?.querySkuDetailsAsync(params) { billingResult, skuDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK &&
                !skuDetailsList.isNullOrEmpty()
            ) {
                val product = GoogleProduct(skuDetails = skuDetailsList.first(), type = type)
                mainScope.launch(dispatcher.main()) {
                    queriedProductLiveData.postValue(product)
                    queriedProductStateFlow.emit(product)
                }
            }
        }
        return GoogleProductQuery(sku, type, this)
    }

    internal fun queryProductLiveData(): LiveData<Productz?> {
        return queriedProductLiveData
    }

    internal fun queryProductStateFlow(): StateFlow<Productz?> {
        return queriedProductState
    }

    internal fun queryProductFlow(sku: String, type: Productz.Type): Flow<Productz?> =
        callbackFlow {
            val stateFlow = MutableStateFlow(null)
            val state = stateFlow.asStateFlow()

            val skuType = when (type) {
                Productz.Type.CONSUMABLE -> BillingClient.SkuType.INAPP
                Productz.Type.NON_CONSUMABLE -> BillingClient.SkuType.INAPP
                Productz.Type.SUBSCRIPTION -> BillingClient.SkuType.SUBS
                else -> {
                    BillingClient.SkuType.INAPP
                }
            }
            val builder = SkuDetailsParams.newBuilder()
            val params = builder
                .setSkusList(listOf(sku))
                .setType(skuType)
                .build()
            client.getBillingClient()
                ?.querySkuDetailsAsync(params) { billingResult, skuDetailsList ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK &&
                        !skuDetailsList.isNullOrEmpty()
                    ) {
                        val product =
                            GoogleProduct(skuDetails = skuDetailsList.first(), type = type)
                        mainScope.launch(dispatcher.main()) {
                            trySend(product)
                        }
                    }
                }
        }

    internal fun queryInventoryLiveData(): LiveData<ArrayMap<String, Productz>?> {
        return requestedProductsLiveData
    }

    internal fun queryInventoryStateFlow(): StateFlow<ArrayMap<String, Productz>?> {
        return requestedProductsState
    }

    override fun queryInventory(products: Map<String, Productz.Type>): QueryResult<Map<String, Productz>> {
        LogUtilz.log.i(
            TAG,
            "queryInventory(" +
                "\n products: ${products.size}," +
                "\n )"
        )
        allProducts = products // todo

        if (products.isNotEmpty()) {
            mainScope.launch {
                LogUtilz.log.d(TAG, "inventory coroutines starting")

                val consumables = mutableListOf<String>()
                val nonConsumables = mutableListOf<String>()
                val subscriptions = mutableListOf<String>()

                withContext(dispatcher.io()) {
                    products.forEach { entry ->
                        when (entry.value) {
                            Productz.Type.CONSUMABLE -> consumables.add(entry.key)
                            Productz.Type.NON_CONSUMABLE -> nonConsumables.add(entry.key)
                            Productz.Type.SUBSCRIPTION -> subscriptions.add(entry.key)
                            else -> {
                                LogUtilz.log.w(TAG, "Unknown Type: ${entry.key}")
                            }
                        }
                    }
                }
                launch(dispatcher.io()) {
                    LogUtilz.log.d(TAG, "inventory coroutines consumables queried")
                    queryProducts(skus = consumables, type = Productz.Type.CONSUMABLE)
                }
                launch(dispatcher.io()) {
                    LogUtilz.log.d(TAG, "inventory coroutines nonConsumables queried")
                    queryProducts(skus = nonConsumables, type = Productz.Type.NON_CONSUMABLE)
                }
                launch(dispatcher.io()) {
                    LogUtilz.log.d(TAG, "inventory coroutines subscriptions queried")
                    queryProducts(skus = subscriptions, type = Productz.Type.SUBSCRIPTION)
                }
            }
        }
        return GoogleInventoryQuery(this)
    }

    @Synchronized
    fun updateConsumableInventory(sku: String, p: Productz) {
        consumables.putIfAbsent(sku, p)
    }

    @Synchronized
    private fun updateNonConsumableInventory(sku: String, p: Productz) {
        nonConsumables.putIfAbsent(sku, p)
    }

    @Synchronized
    private fun updateSubscriptionInventory(sku: String, p: Productz) {
        subscriptions.putIfAbsent(sku, p)
    }

    override fun updateInventory(products: List<Productz>?, type: Productz.Type) {
        LogUtilz.log.i(
            TAG,
            "updateInventory(" +
                "\n products: ${products?.size ?: 0}," +
                "\n type: $type," +
                "\n )"
        )
        if (!products.isNullOrEmpty()) {
            mainScope.launch(dispatcher.io()) {
                for (p in products) {
                    when (p.type) {
                        Productz.Type.CONSUMABLE -> {
                            p.sku?.let { sku ->
                                updateConsumableInventory(sku, p)
                            }
                        }
                        Productz.Type.NON_CONSUMABLE -> {
                            p.sku?.let { sku ->
                                updateNonConsumableInventory(sku, p)
                            }
                        }
                        Productz.Type.SUBSCRIPTION -> {
                            p.sku?.let { sku ->
                                updateSubscriptionInventory(sku, p)
                            }
                        }
                        else -> {
                            LogUtilz.log.w(TAG, "Unhandled product type: ${p.type}.")
                        }
                    }
                }

                when (type) {
                    Productz.Type.CONSUMABLE -> {
                        requestedProductsLiveData.postValue(consumables)
                        requestedProductsStateFlow.emit(consumables)
                    }
                    Productz.Type.NON_CONSUMABLE -> {
                        requestedProductsLiveData.postValue(nonConsumables)
                        requestedProductsStateFlow.emit(nonConsumables)
                    }
                    Productz.Type.SUBSCRIPTION -> {
                        requestedProductsLiveData.postValue(subscriptions)
                        requestedProductsStateFlow.emit(subscriptions)
                    }
                    else -> {
                        LogUtilz.log.w(TAG, "Unhandled product type: $type")
                        val unidentifiedProducts: ArrayMap<String, Productz> = ArrayMap()
                        unidentifiedProducts.putAll(from = consumables)
                        unidentifiedProducts.putAll(from = nonConsumables)
                        unidentifiedProducts.putAll(from = subscriptions)
                        requestedProductsLiveData.postValue(unidentifiedProducts)
                        requestedProductsStateFlow.emit(unidentifiedProducts)
                    }
                }

                isReadyLiveData.postValue(true)
                isReadyStateFlow.emit(true)
            }
        }
    }

    @Synchronized
    private fun getConsumableProduct(sku: String?): Productz? {
        if(consumables.containsKey(sku))
            return consumables[sku]
        return null
    }

    @Synchronized
    private fun getNonConsumableProduct(sku: String?): Productz? {
        if(nonConsumables.containsKey(sku))
            return nonConsumables[sku]
        return null
    }

    @Synchronized
    private fun getSubscriptionProduct(sku: String?): Productz? {
        if(subscriptions.containsKey(sku))
            return subscriptions[sku]
        return null
    }

    override fun getProduct(sku: String?): Productz? {
        LogUtilz.log.v(TAG, "getProduct: $sku")
        getConsumableProduct(sku)?.let{ return it }
        getNonConsumableProduct(sku)?.let{ return it }
        getSubscriptionProduct(sku)?.let{ return it }
        return null
    }

    override fun getProducts(
        type: Productz.Type?,
        promo: Productz.Promotion?
    ): Map<String, Productz> {
        when (type) {
            Productz.Type.CONSUMABLE -> {
                if (promo != null) {
                    val promos = ArrayMap<String, Productz>()
                    consumables.forEach { entry ->
                        if (entry.value.promotion == promo) {
                            promos[entry.key] = entry.value
                        }
                        return promos
                    }
                }
                return consumables
            }
            Productz.Type.NON_CONSUMABLE -> {
                if (promo != null) {
                    val promos = ArrayMap<String, Productz>()
                    nonConsumables.forEach { entry ->
                        if (entry.value.promotion == promo) {
                            promos[entry.key] = entry.value
                        }
                        return promos
                    }
                }
                return nonConsumables
            }
            Productz.Type.SUBSCRIPTION -> {
                if (promo != null) {
                    val promos = ArrayMap<String, Productz>()
                    subscriptions.forEach { entry ->
                        if (entry.value.promotion == promo) {
                            promos[entry.key] = entry.value
                        }
                        return promos
                    }
                }
                return subscriptions
            }
            else -> {
                val all = ArrayMap<String, Productz>()
                all.putAll(from = consumables)
                all.putAll(from = nonConsumables)
                all.putAll(from = subscriptions)
                return all
            }
        }
    }

    override fun isReadyStateFlow(): StateFlow<Boolean> {
        // we can assume that the requestedProductsLiveData must not be empty
        // since a successful update of the inventory by the billing library
        // will always publish updates to it.
        mainScope.launch {
            val isCacheReady = !requestedProductsLiveData.value.isNullOrEmpty()
            LogUtilz.log.d(TAG, "isInventoryCacheReady: $isCacheReady")
            isReadyStateFlow.emit(isCacheReady)
        }
        return isReadyState
    }

    override fun isReadyLiveData(): LiveData<Boolean> {
        isReadyLiveData.postValue(!requestedProductsLiveData.value.isNullOrEmpty())
        return isReadyLiveData
    }

    override fun destroy() {
        LogUtilz.log.v(TAG, "destroy")
    }

    companion object {
        private const val TAG = "BillingzGoogleInventory"
    }
}
