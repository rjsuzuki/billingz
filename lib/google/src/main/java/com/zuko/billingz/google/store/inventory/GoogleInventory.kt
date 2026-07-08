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
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.UnfetchedProduct
import com.zuko.billingz.core.misc.BillingzDispatcher
import com.zuko.billingz.core.misc.Dispatcherz
import com.zuko.billingz.core.misc.Logger
import com.zuko.billingz.core.store.inventory.Inventoryz
import com.zuko.billingz.core.store.model.Productz
import com.zuko.billingz.core.store.model.QueryResult
import com.zuko.billingz.google.store.client.GoogleClient
import com.zuko.billingz.google.store.model.GoogleInventoryQuery
import com.zuko.billingz.google.store.model.GoogleProduct
import com.zuko.billingz.google.store.model.GoogleProductQuery
import com.zuko.billingz.google.store.sales.GoogleResponse
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GoogleInventory(
    private val client: GoogleClient,
    private val dispatcher: Dispatcherz = BillingzDispatcher()
) : Inventoryz {
    override var isNewVersion = false
    override var allProducts: Map<String, Productz.Type> = ArrayMap()

    override var consumables: ArrayMap<String, Productz> = ArrayMap()
    override var nonConsumables: ArrayMap<String, Productz> = ArrayMap()
    override var subscriptions: ArrayMap<String, Productz> = ArrayMap()

    private val isReadyLiveData = MutableLiveData<Boolean>()
    private val isReadyStateFlow = MutableStateFlow(false)
    private val isReadyState = isReadyStateFlow.asStateFlow()

    private val requestedProductsLiveData: MutableLiveData<ArrayMap<String, Productz>> by lazy {
        MutableLiveData()
    }
    private val requestedProductsStateFlow: MutableStateFlow<ArrayMap<String, Productz>> by lazy {
        MutableStateFlow(
            ArrayMap()
        )
    }
    private val requestedProductsState: StateFlow<ArrayMap<String, Productz>> by lazy { requestedProductsStateFlow.asStateFlow() }

    private val mainScope = MainScope()

    private fun queryProducts(skus: List<String>, type: Productz.Type) {
        Logger.v(TAG, "queryProducts")
        if (skus.isEmpty()) {
            Logger.w(TAG, "Cannot run a query with an empty list of: $type")
            return
        }

        val skuType = when (type) {
            Productz.Type.CONSUMABLE -> BillingClient.ProductType.INAPP
            Productz.Type.NON_CONSUMABLE -> BillingClient.ProductType.INAPP
            Productz.Type.SUBSCRIPTION -> BillingClient.ProductType.SUBS
            else -> {
                BillingClient.ProductType.INAPP
            }
        }

        val builder = QueryProductDetailsParams.newBuilder()
        val list = skus.map { sku ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(sku)
                .setProductType(skuType)
                .build()
        }
        val params = builder
            .setProductList(list)
            .build()

        client.getBillingClient()?.queryProductDetailsAsync(params) { billingResult, queryProductDetailsResult ->
            handleQueryResult(
                result = billingResult,
                productDetailsList = queryProductDetailsResult.productDetailsList,
                unfetchedProducts = queryProductDetailsResult.unfetchedProductList,
                type = type
            )
        }
    }

    private fun handleQueryResult(
        result: BillingResult?,
        productDetailsList: List<ProductDetails>?,
        unfetchedProducts: List<UnfetchedProduct>?,
        type: Productz.Type
    ) {
        Logger.v(TAG, "Processing inventory query result...")
        Logger.d(
            TAG,
            "handleQueryResult =>" +
                "\n type: $type," +
                "\n billingResult code: ${result?.responseCode}," +
                "\n billingResult msg: ${result?.debugMessage ?: "n/a"}," +
                "\n productDetails: $productDetailsList" +
                "\n unfetchedProducts: $unfetchedProducts" +
                "\n -----------------------------------"
        )
        if (!unfetchedProducts.isNullOrEmpty()) {
            Logger.w(TAG, "Some products could not be fetched: $unfetchedProducts")
        }
        if (result?.responseCode == BillingClient.BillingResponseCode.OK &&
            !productDetailsList.isNullOrEmpty()
        ) {
            val availableProducts = mutableListOf<Productz>()
            for (p in productDetailsList) {
                val product = GoogleProduct(productDetails = p, type = type)
                availableProducts.add(product)
            }
            updateInventory(products = availableProducts, type = type)
        } else {
            Logger.w(TAG, "Cannot fetch product details list => responseCode: ${result?.responseCode}")
        }
    }

    override fun queryProduct(sku: String, type: Productz.Type): QueryResult<Productz> {
        Logger.v(
            TAG,
            "queryProduct =>" +
                "\n sku: $sku," +
                "\n type: $type"
        )
        val skuType = when (type) {
            Productz.Type.CONSUMABLE -> BillingClient.ProductType.INAPP
            Productz.Type.NON_CONSUMABLE -> BillingClient.ProductType.INAPP
            Productz.Type.SUBSCRIPTION -> BillingClient.ProductType.SUBS
            Productz.Type.UNKNOWN -> Productz.Type.UNKNOWN.name
        }

        val query = GoogleProductQuery(sku, type)

        if (skuType == Productz.Type.UNKNOWN.name) {
            queryProductInternal(sku, Productz.Type.SUBSCRIPTION, BillingClient.ProductType.SUBS, query)
            queryProductInternal(sku, Productz.Type.CONSUMABLE, BillingClient.ProductType.INAPP, query)
            queryProductInternal(sku, Productz.Type.NON_CONSUMABLE, BillingClient.ProductType.INAPP, query)
        } else {
            queryProductInternal(sku, type, skuType, query)
        }

        return query
    }

    private fun queryProductInternal(sku: String, type: Productz.Type, skuType: String, query: GoogleProductQuery) {
        val builder = QueryProductDetailsParams.newBuilder()
        val params = builder
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(sku)
                        .setProductType(skuType)
                        .build()
                )
            )
            .build()
        client.getBillingClient()?.queryProductDetailsAsync(params) { billingResult, queryProductDetailsResult ->
            val productDetailsList = queryProductDetailsResult.productDetailsList
            Logger.d(
                TAG,
                "queryProduct =>" +
                    "\n billingResult.responseCode: ${billingResult.responseCode}," +
                    "\n billingResult.debugMessage: ${billingResult.debugMessage}," +
                    "\n productDetailsList: $productDetailsList," +
                    "\n unfetchedProducts: ${queryProductDetailsResult.unfetchedProductList}"
            )
            GoogleResponse.logResult(billingResult)
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK &&
                productDetailsList.isNotEmpty()
            ) {
                val product = GoogleProduct(productDetails = productDetailsList.first(), type = type)
                when (product.type) {
                    Productz.Type.UNKNOWN -> Logger.wtf(TAG, "queryProductInternal => Cannot update inventory with an unknown product type")
                    Productz.Type.CONSUMABLE -> consumables.putIfAbsent(sku, product)
                    Productz.Type.NON_CONSUMABLE -> nonConsumables.putIfAbsent(sku, product)
                    Productz.Type.SUBSCRIPTION -> subscriptions.putIfAbsent(sku, product)
                }
                mainScope.launch(dispatcher.main()) {
                    query.queriedProductLiveData.postValue(product)
                    query.queriedProductStateFlow.emit(product)
                }
            } else {
                mainScope.launch(dispatcher.main()) {
                    query.queriedProductLiveData.postValue(null)
                    query.queriedProductStateFlow.emit(null)
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
        Logger.d(
            TAG,
            "queryInventory(" +
                "\n products: ${products.size}," +
                "\n )"
        )
        allProducts = products // todo: consider deprecating this as it's purpose is slightly redundant

        if (products.isNotEmpty()) {
            mainScope.launch {
                Logger.d(TAG, "inventory coroutines starting")

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
                                Logger.w(TAG, "Unknown Type: ${entry.key}")
                            }
                        }
                    }
                }
                launch(dispatcher.io()) {
                    Logger.d(TAG, "inventory coroutines consumables queried")
                    queryProducts(skus = consumables, type = Productz.Type.CONSUMABLE)
                }
                launch(dispatcher.io()) {
                    Logger.d(TAG, "inventory coroutines non-consumables queried")
                    queryProducts(skus = nonConsumables, type = Productz.Type.NON_CONSUMABLE)
                }
                launch(dispatcher.io()) {
                    Logger.d(TAG, "inventory coroutines subscriptions queried")
                    queryProducts(skus = subscriptions, type = Productz.Type.SUBSCRIPTION)
                }
            }
        }
        return GoogleInventoryQuery(this)
    }

    override fun updateInventory(products: List<Productz>?, type: Productz.Type) {
        Logger.i(
            TAG,
            "updateInventory(" +
                "\n products: ${products?.size ?: 0}," +
                "\n type: $type," +
                "\n )"
        )

        if (!products.isNullOrEmpty()) {
            mainScope.launch(dispatcher.io()) {
                val tempConsumables: ArrayMap<String, Productz> = ArrayMap()
                val tempNonConsumables: ArrayMap<String, Productz> = ArrayMap()
                val tempSubscriptions: ArrayMap<String, Productz> = ArrayMap()
                for (p in products) {
                    when (p.type) {
                        Productz.Type.CONSUMABLE -> {
                            p.getProductId()?.let { sku ->
                                tempConsumables.putIfAbsent(sku, p)
                            }
                        }
                        Productz.Type.NON_CONSUMABLE -> {
                            p.getProductId()?.let { sku ->
                                tempNonConsumables.putIfAbsent(sku, p)
                            }
                        }
                        Productz.Type.SUBSCRIPTION -> {
                            p.getProductId()?.let { sku ->
                                tempSubscriptions.putIfAbsent(sku, p)
                            }
                        }
                        else -> {
                            Logger.w(TAG, "Unhandled product type: ${p.type}.")
                        }
                    }
                }

                updateConsumbalesCache(tempConsumables)
                updateNonConsumbalesCache(tempNonConsumables)
                updateSubscriptionsCache(tempSubscriptions)

                when (type) {
                    Productz.Type.CONSUMABLE -> {
                        requestedProductsLiveData.postValue(tempConsumables)
                        requestedProductsStateFlow.emit(tempConsumables)
                    }
                    Productz.Type.NON_CONSUMABLE -> {
                        requestedProductsLiveData.postValue(tempNonConsumables)
                        requestedProductsStateFlow.emit(tempNonConsumables)
                    }
                    Productz.Type.SUBSCRIPTION -> {
                        requestedProductsLiveData.postValue(tempSubscriptions)
                        requestedProductsStateFlow.emit(tempSubscriptions)
                    }
                    else -> {
                        Logger.w(TAG, "Unhandled product type: $type")
                        val unidentifiedProducts: ArrayMap<String, Productz> = ArrayMap()
                        unidentifiedProducts.putAll(from = tempNonConsumables)
                        unidentifiedProducts.putAll(from = tempNonConsumables)
                        unidentifiedProducts.putAll(from = tempSubscriptions)
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
    private fun updateConsumbalesCache(data: ArrayMap<String, Productz>) {
        data.forEach { c -> consumables.putIfAbsent(c.key, c.value) }
    }

    @Synchronized
    private fun updateNonConsumbalesCache(data: ArrayMap<String, Productz>) {
        data.forEach { nc -> nonConsumables.putIfAbsent(nc.key, nc.value) }
    }

    @Synchronized
    private fun updateSubscriptionsCache(data: ArrayMap<String, Productz>) {
        data.forEach { s -> subscriptions.putIfAbsent(s.key, s.value) }
    }

    override fun getProduct(sku: String?): Productz? {
        Logger.v(TAG, "getProduct: $sku")
        if (consumables.containsKey(sku)) {
            return consumables[sku]
        }
        if (nonConsumables.containsKey(sku)) {
            return nonConsumables[sku]
        }
        if (subscriptions.containsKey(sku)) {
            return subscriptions[sku]
        }
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
                        if (entry.value.getPromotion() == promo) {
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
                        if (entry.value.getPromotion() == promo) {
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
                        if (entry.value.getPromotion() == promo) {
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
            Logger.d(TAG, "isInventoryCacheReady: $isCacheReady")
            isReadyStateFlow.emit(isCacheReady)
        }
        return isReadyState
    }

    override fun isReadyLiveData(): LiveData<Boolean> {
        isReadyLiveData.postValue(!requestedProductsLiveData.value.isNullOrEmpty())
        return isReadyLiveData
    }

    override fun destroy() {
        Logger.v(TAG, "destroy")
        mainScope.cancel()
        consumables.clear()
        nonConsumables.clear()
        subscriptions.clear()
    }

    companion object {
        private const val TAG = "BillingzGoogleInventory"
    }
}
