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
package com.zuko.billingz.amazon.store.inventory

import android.util.ArrayMap
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.amazon.device.iap.PurchasingService
import com.amazon.device.iap.model.ProductDataResponse
import com.amazon.device.iap.model.ProductType
import com.zuko.billingz.amazon.store.model.AmazonProduct
import com.zuko.billingz.core.LogUtilz
import com.zuko.billingz.core.store.model.Productz
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow

class AmazonInventory : AmazonInventoryz {

    override var allProducts: Map<String, Productz.Type> = ArrayMap()

    override var consumables: Map<String, Productz> = ArrayMap()
    override var nonConsumables: Map<String, Productz> = ArrayMap()
    override var subscriptions: Map<String, Productz> = ArrayMap()
    override var requestedProducts: MutableLiveData<Map<String, Productz>> = MutableLiveData()

    private var queryType: Productz.Type = Productz.Type.UNKNOWN
    private val mainScope by lazy { MainScope() }

    override suspend fun queryProduct(sku: String, type: Productz.Type): Productz? {
        val skuType = when (type) {
            Productz.Type.CONSUMABLE -> ProductType.CONSUMABLE.name
            Productz.Type.NON_CONSUMABLE -> ProductType.ENTITLED.name
            Productz.Type.SUBSCRIPTION -> ProductType.SUBSCRIPTION.name
            else -> Productz.Type.UNKNOWN.name
        }

        // check if in cache - local
        // check against server - remote
        val set = mutableSetOf<String>()
        set.add(sku)
        PurchasingService.getProductData(set)
        queryType = type
        return null // todo
    }

    override fun handleQueriedProducts(response: ProductDataResponse?) {
        when (response?.requestStatus) {
            ProductDataResponse.RequestStatus.SUCCESSFUL -> {
                LogUtilz.log.d(
                    TAG,
                    "Successful product data request: ${response.requestId}"
                )

                // convert
                val productsList = mutableListOf<Productz>()
                val products = androidx.collection.ArrayMap<String, Productz.Type>()
                for (r in response.productData) {
                    val product = AmazonProduct(r.value)
                    products[r.key] = product.type
                    productsList.add(product)
                    LogUtilz.log.v(TAG, "Validated product: $product")
                }
                allProducts = products
                updateInventory(productsList, Productz.Type.UNKNOWN)
                // cache
                val unavailableSkusSet = response.unavailableSkus // todo
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

    override fun queryProductFlow(sku: String, type: Productz.Type): Flow<Productz> {
        throw NotImplementedError()
    }

    override fun queryInventory(products: Map<String, Productz.Type>): LiveData<Map<String, Productz>> {
        // Call this method to retrieve item data for a set of SKUs to display in your app.
        // Call getProductData in the OnResume method.
        val productDataRequestId = PurchasingService.getProductData(products.keys.toSet()) // inventory
        Log.v(TAG, "get product data request: $productDataRequestId")
        return requestedProducts
    }

    override fun updateInventory(products: List<Productz>?, type: Productz.Type) {
        Log.d(TAG, "updateInventory : ${products?.size ?: 0}")
        if (!products.isNullOrEmpty()) {
            consumables = consumables + products.associateBy { it.sku.toString() }
            nonConsumables = nonConsumables + products.associateBy { it.sku.toString() }
            subscriptions = subscriptions + products.associateBy { it.sku.toString() }

            when (type) {
                Productz.Type.CONSUMABLE -> {
                    requestedProducts.postValue(consumables)
                }
                Productz.Type.NON_CONSUMABLE -> {
                    requestedProducts.postValue(nonConsumables)
                }
                Productz.Type.SUBSCRIPTION -> {
                    requestedProducts.postValue(subscriptions)
                }
                else -> {
                    requestedProducts.postValue(consumables)
                    LogUtilz.log.w(TAG, "Unhandled product type: $type. Defauling to consumables.")
                }
            }
        }
    }

    fun getAvailableProducts(
        skuList: MutableList<String>,
        productType: Productz.Type?
    ): LiveData<Map<String, Productz>> {

        when (productType) {
            Productz.Type.CONSUMABLE -> {
                // consumables = consumables + products.associateBy { it.sku.toString() }
                requestedProducts.postValue(consumables)
            }
            Productz.Type.NON_CONSUMABLE -> {
                // nonConsumables = nonConsumables + products.associateBy { it.sku.toString() }
                requestedProducts.postValue(nonConsumables)
            }
            Productz.Type.SUBSCRIPTION -> {
                // subscriptions = subscriptions + products.associateBy { it.sku.toString() }
                requestedProducts.postValue(subscriptions)
            }
            else -> {
            }
        }
        return requestedProducts
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

    override fun getProducts(type: Productz.Type?, promo: Productz.Promotion?): Map<String, Productz> {
        when (type) {
            Productz.Type.CONSUMABLE -> {
                if (promo != null) {
                    consumables.forEach { entry ->
                        val promos = ArrayMap<String, Productz>()
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
                    nonConsumables.forEach { entry ->
                        val promos = ArrayMap<String, Productz>()
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
                    subscriptions.forEach { entry ->
                        val promos = ArrayMap<String, Productz>()
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
                all.putAll(consumables)
                all.putAll(nonConsumables)
                all.putAll(subscriptions)
                return all
            }
        }
    }

    override fun destroy() {
        LogUtilz.log.v(TAG, "destroy")
        mainScope.cancel()
    }

    companion object {
        private const val TAG = "AmazonInventory"
    }
}
