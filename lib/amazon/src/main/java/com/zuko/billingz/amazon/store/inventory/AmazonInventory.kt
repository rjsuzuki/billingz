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
import com.zuko.billingz.core.LogUtilz
import com.zuko.billingz.core.misc.BillingzDispatcher
import com.zuko.billingz.core.misc.Dispatcherz
import com.zuko.billingz.core.store.model.Productz
import com.zuko.billingz.core.store.model.QueryResult
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
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

    private val queriedProductLiveData = MutableLiveData<AmazonProduct?>()
    private val queriedProductStateFlow = MutableStateFlow<AmazonProduct?>(null)
    private val queriedProductState = queriedProductStateFlow.asStateFlow()

    /**
     * Cached list of invalid skus to prevent your app's users from being able
     * to purchase these products.
     */
    override var unavailableSkus: Set<String>? = null

    private var queryType: Productz.Type = Productz.Type.UNKNOWN
    private val mainScope by lazy { MainScope() }

    override fun processQueriedProducts(response: ProductDataResponse?) {
        LogUtilz.log.v(
            TAG,
            "handleQueriedProducts:" +
                "\nrequest id: ${response?.requestId}," +
                "\nstatus: ${response?.requestStatus}," +
                "\nunavailableSkus: ${response?.unavailableSkus?.size}," +
                "\nproducts: ${response?.productData}"
        )
        when (response?.requestStatus) {
            ProductDataResponse.RequestStatus.SUCCESSFUL -> {
                LogUtilz.log.d(
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
                    LogUtilz.log.d(TAG, "Converted AmazonProduct: $product")
                    products[r.key] = product.type
                    productsList.add(product)
                    LogUtilz.log.i(TAG, "Validated product: $product")
                }
                LogUtilz.log.v(
                    TAG,
                    "allProducts: ${allProducts.size}, productsList: ${productsList.size}"
                )
                allProducts = products
                updateInventory(productsList, Productz.Type.UNKNOWN)
                unavailableSkus = response.unavailableSkus
            }
            ProductDataResponse.RequestStatus.FAILED -> {
                LogUtilz.log.e(
                    TAG,
                    "Failed product data request: ${response.requestId}"
                )
            }
            ProductDataResponse.RequestStatus.NOT_SUPPORTED -> {
                LogUtilz.log.wtf(
                    TAG,
                    "Unsupported product data request: ${response.requestId}"
                )
            }
            else -> {
                LogUtilz.log.w(
                    TAG,
                    "Unknown request status: ${response?.requestId}"
                )
            }
        }
        queryType = Productz.Type.UNKNOWN
    }

    internal fun queryProductStateFlow(): StateFlow<AmazonProduct?> {
        return queriedProductState
    }

    internal fun queryProductLiveData(): LiveData<AmazonProduct?> {
        return queriedProductLiveData
    }

    override fun queryProduct(sku: String, type: Productz.Type): QueryResult<Productz> {
        mainScope.launch(dispatcher.io()) {
            val skuType = when (type) {
                Productz.Type.CONSUMABLE -> ProductType.CONSUMABLE.name
                Productz.Type.NON_CONSUMABLE -> ProductType.ENTITLED.name
                Productz.Type.SUBSCRIPTION -> ProductType.SUBSCRIPTION.name
                else -> ProductType.SUBSCRIPTION.name
            }

            // check if in cache - local
            // check against server - remote
            val set = mutableSetOf<String>()
            set.add(skuType)
            queryType = type
            try {
                PurchasingService.getProductData(set)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return AmazonProductQuery(sku, type, this)
    }

    override fun queryInventory(products: Map<String, Productz.Type>): QueryResult<Map<String, Productz>> {
        LogUtilz.log.v(TAG, "queryInventory")
        // Call this method to retrieve item data for a set of SKUs to display in your app.
        // Call getProductData in the OnResume method.
        try {
            val productDataRequestId =
                PurchasingService.getProductData(products.keys.toSet()) // inventory
            LogUtilz.log.i(
                TAG,
                "get product data request: $productDataRequestId," +
                        "products: $products"
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return AmazonInventoryQuery(this)
    }

    internal fun queryInventoryLiveData(): LiveData<ArrayMap<String, Productz>?> {
        LogUtilz.log.v(TAG, "queryInventoryLiveData")
        return requestedProductsLiveData
    }

    internal fun queryInventoryStateFlow(): StateFlow<ArrayMap<String, Productz>?> {
        LogUtilz.log.v(TAG, "queryInventoryStateFlow")
        return requestedProductsState
    }

    override fun updateInventory(products: List<Productz>?, type: Productz.Type) {
        LogUtilz.log.d(TAG, "updateInventory : ${products?.size ?: 0}")
        if (!products.isNullOrEmpty()) {
            mainScope.launch(dispatcher.io()) {
                // Unlike the Android Billing Library, the amazon sdk does
                // not provide functionality to run queries by product type.
                for (p in products) {
                    when (p.type) {
                        Productz.Type.CONSUMABLE -> {
                            p.getProductId()?.let { sku ->
                                consumables.putIfAbsent(sku, p)
                            }
                        }
                        Productz.Type.NON_CONSUMABLE -> {
                            p.getProductId()?.let { sku ->
                                nonConsumables.putIfAbsent(sku, p)
                            }
                        }
                        Productz.Type.SUBSCRIPTION -> {
                            p.getProductId()?.let { sku ->
                                subscriptions.putIfAbsent(sku, p)
                            }
                        }
                        else -> {
                            LogUtilz.log.w(TAG, "Unhandled product type: ${p.type}.")
                        }
                    }
                }

                // After sorting the skus into the inventory cache, notify subscribers
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
                        LogUtilz.log.w(TAG, "Unknown product type: $type. Defaulting to all.")
                        val unidentifiedProducts: ArrayMap<String, Productz> = ArrayMap()
                        unidentifiedProducts.putAll(from = consumables)
                        unidentifiedProducts.putAll(from = nonConsumables)
                        unidentifiedProducts.putAll(from = subscriptions)
                        requestedProductsLiveData.postValue(unidentifiedProducts)
                        requestedProductsStateFlow.emit(unidentifiedProducts)
                    }
                }
            }
        }
    }

    override fun getProduct(sku: String?): Productz? {
        if (consumables.containsKey(sku))
            return consumables[sku]
        if (nonConsumables.containsKey(sku))
            return nonConsumables[sku]
        if (subscriptions.containsKey(sku))
            return subscriptions[sku]
        return null
    }

    override fun getProducts(
        type: Productz.Type?,
        promo: Productz.Promotion?
    ): Map<String, Productz> {
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
        mainScope.cancel()
    }

    companion object {
        private const val TAG = "AmazonInventory"
    }
}
