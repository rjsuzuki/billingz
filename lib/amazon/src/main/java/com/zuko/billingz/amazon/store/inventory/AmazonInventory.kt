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
package com.zuko.billingz.amazon.store.inventory

import androidx.collection.ArrayMap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.amazon.device.iap.PurchasingService
import com.amazon.device.iap.model.ProductDataResponse
import com.amazon.device.iap.model.ProductType
import com.zuko.billingz.amazon.store.model.AmazonInventoryQuery
import com.zuko.billingz.amazon.store.model.AmazonProduct
import com.zuko.billingz.amazon.store.model.AmazonProductQuery
import com.zuko.billingz.core.misc.BillingzDispatcher
import com.zuko.billingz.core.misc.Dispatcherz
import com.zuko.billingz.core.misc.Logger
import com.zuko.billingz.core.store.model.Productz
import com.zuko.billingz.core.store.model.QueryResult
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AmazonInventory(
    private val dispatcher: Dispatcherz = BillingzDispatcher()
) : AmazonInventoryz {

    override var isNewVersion = false
    override var allProducts: Map<String, Productz.Type> = ArrayMap()

    override var consumables: ArrayMap<String, Productz> = ArrayMap()
    override var nonConsumables: ArrayMap<String, Productz> = ArrayMap()
    override var subscriptions: ArrayMap<String, Productz> = ArrayMap()

    private val isReadyLiveData = MutableLiveData<Boolean>()
    private val isReadyStateFlow = MutableStateFlow(false)
    private val isReadyState = isReadyStateFlow.asStateFlow()
    private var requestedProductsLiveData: MutableLiveData<ArrayMap<String, Productz>> =
        MutableLiveData()
    private val requestedProductsStateFlow: MutableStateFlow<ArrayMap<String, Productz>> by lazy {
        MutableStateFlow(
            ArrayMap()
        )
    }
    private val requestedProductsState: StateFlow<ArrayMap<String, Productz>> by lazy { requestedProductsStateFlow.asStateFlow() }

    private val queriedProductsMap = ArrayMap<String, AmazonProductQuery>()

    /**
     * Cached list of invalid skus to prevent your app's users from being able
     * to purchase these products.
     */
    override var unavailableSkus: Set<String>? = null

    private var queryType: Productz.Type = Productz.Type.UNKNOWN
    private val mainScope by lazy { MainScope() }

    override fun processQueriedProducts(response: ProductDataResponse?) {
        Logger.d(
            TAG,
            "processQueriedProducts:" +
                "\nrequest id: ${response?.requestId}," +
                "\nstatus: ${response?.requestStatus}," +
                "\nunavailableSkus: ${response?.unavailableSkus?.size}," +
                "\nproducts: ${response?.productData}"
        )

        response?.unavailableSkus?.forEach { sku ->
            Logger.w(TAG, "Unavailable Product(SKU): $sku")
        }

        when (response?.requestStatus) {
            ProductDataResponse.RequestStatus.SUCCESSFUL -> {
                Logger.d(
                    TAG,
                    "Successful product data request: ${response.requestId}"
                )
                // convert
                val productsList = mutableListOf<Productz>()
                val products = ArrayMap<String, Productz.Type>()
                for (r in response.productData) {
                    val type = when (r.value.productType) {
                        ProductType.CONSUMABLE -> Productz.Type.CONSUMABLE
                        ProductType.ENTITLED -> Productz.Type.NON_CONSUMABLE
                        ProductType.SUBSCRIPTION -> Productz.Type.SUBSCRIPTION
                        else -> Productz.Type.UNKNOWN
                    }
                    val product = AmazonProduct(r.value, type)
                    Logger.d(TAG, "Converted AmazonProduct: $product")
                    products[r.key] = product.type
                    productsList.add(product)
                    Logger.i(TAG, "Validated product: $product")
                    product.getProductId()?.let { sku ->
                        mainScope.launch(dispatcher.main()) {
                            queriedProductsMap[sku]?.let { query ->
                                query.queriedProductLiveData.postValue(product)
                                query.queriedProductStateFlow.emit(product)
                                queriedProductsMap.remove(sku)
                            }
                        }
                    }
                }
                Logger.v(
                    TAG,
                    "allProducts: ${allProducts.size}, productsList: ${productsList.size}"
                )
                allProducts = products
                updateInventory(productsList, Productz.Type.UNKNOWN)
                unavailableSkus = response.unavailableSkus
            }
            ProductDataResponse.RequestStatus.FAILED -> {
                Logger.e(
                    TAG,
                    "Failed product data request: ${response.requestId}"
                )
            }
            ProductDataResponse.RequestStatus.NOT_SUPPORTED -> {
                Logger.wtf(
                    TAG,
                    "Unsupported product data request: ${response.requestId}"
                )
            }
            else -> {
                Logger.w(
                    TAG,
                    "Unknown request status: ${response?.requestId}"
                )
            }
        }
        queryType = Productz.Type.UNKNOWN
    }

    override fun queryProduct(sku: String, type: Productz.Type): QueryResult<Productz> {
        Logger.v(
            TAG,
            "queryProduct =>" +
                "\n sku: $sku, " +
                "\n type: $type"
        )
        mainScope.launch(dispatcher.io()) {
            val skuType = when (type) {
                Productz.Type.CONSUMABLE -> ProductType.CONSUMABLE.name
                Productz.Type.NON_CONSUMABLE -> ProductType.ENTITLED.name
                Productz.Type.SUBSCRIPTION -> ProductType.SUBSCRIPTION.name
                Productz.Type.UNKNOWN -> Productz.Type.UNKNOWN.name
            }

            // check if in cache - local
            // check against server - remote
            val set = mutableSetOf<String>()
            if (skuType == Productz.Type.UNKNOWN.name) {
                set.add(ProductType.SUBSCRIPTION.name)
                set.add(ProductType.ENTITLED.name)
                set.add(ProductType.CONSUMABLE.name)
            } else {
                set.add(skuType)
            }

            queryType = type
            try {
                PurchasingService.getProductData(set)
            } catch (e: Exception) {
                Logger.e(TAG, e)
            }
        }
        val query = AmazonProductQuery(sku, type)
        queriedProductsMap[sku] = query
        return query
    }

    override fun queryInventory(products: Map<String, Productz.Type>): QueryResult<Map<String, Productz>> {
        Logger.v(TAG, "queryInventory: $products")
        // Call this method to retrieve item data for a set of SKUs to display in your app.
        // Call getProductData in the OnResume method.
        try {
            val productDataRequestId =
                PurchasingService.getProductData(products.keys.toSet()) // inventory
            Logger.i(
                TAG,
                "get product data request: $productDataRequestId," +
                    "products: $products"
            )
        } catch (e: Exception) {
            Logger.e(TAG, e)
        }

        return AmazonInventoryQuery(this)
    }

    internal fun queryInventoryLiveData(): LiveData<ArrayMap<String, Productz>?> {
        Logger.i(TAG, "queryInventoryLiveData")
        return requestedProductsLiveData
    }

    internal fun queryInventoryStateFlow(): StateFlow<ArrayMap<String, Productz>?> {
        Logger.i(TAG, "queryInventoryStateFlow")
        return requestedProductsState
    }

    override fun updateInventory(products: List<Productz>?, type: Productz.Type) {
        Logger.v(
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

                // Unlike the Android Billing Library, the amazon sdk does
                // not provide functionality to run queries by product type.
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

                // After sorting the skus into the inventory cache, notify subscribers
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
                        Logger.w(TAG, "Unknown product type: $type. Defaulting to all.")
                        val unidentifiedProducts: ArrayMap<String, Productz> = ArrayMap()
                        unidentifiedProducts.putAll(from = tempConsumables)
                        unidentifiedProducts.putAll(from = tempNonConsumables)
                        unidentifiedProducts.putAll(from = tempSubscriptions)
                        requestedProductsLiveData.postValue(unidentifiedProducts)
                        requestedProductsStateFlow.emit(unidentifiedProducts)
                    }
                }
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
        Logger.i(TAG, "getProduct: $sku")
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
        Logger.i(
            TAG,
            "getProducts =>" +
                "\n type: $type," +
                "\n promo: $promo"
        )
        when (type) {
            Productz.Type.CONSUMABLE -> {
                if (promo != null) {
                    consumables.forEach { entry ->
                        val promos = ArrayMap<String, Productz>()
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
                    nonConsumables.forEach { entry ->
                        val promos = ArrayMap<String, Productz>()
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
                    subscriptions.forEach { entry ->
                        val promos = ArrayMap<String, Productz>()
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
        Logger.v(TAG, "destroying...")
        // mainScope.cancel() // TODO: don't stop?
    }

    companion object {
        private const val TAG = "AmazonInventory"
    }
}
