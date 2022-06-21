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
package com.zuko.billingz.core.store.inventory

import androidx.collection.ArrayMap
import androidx.lifecycle.LiveData
import com.zuko.billingz.core.misc.CleanUpz
import com.zuko.billingz.core.store.model.Productz
import com.zuko.billingz.core.store.model.QueryResult
import kotlinx.coroutines.flow.StateFlow

/**
 * Blueprint for managing a store's (your applications) entire collection of available products (inventory)
 */
interface Inventoryz : CleanUpz {

    /**
     * For providing version compatability changes
     */
    var isNewVersion: Boolean

    /**
     * Current cache of all products that is provided by your app/server. This list may include
     * both verified and unverified skus.
     */
    var allProducts: Map<String, Productz.Type>

    /**
     * Current cache of available consumables.
     */
    var consumables: ArrayMap<String, Productz>

    /**
     * Current cache of available non-consumables.
     */
    var nonConsumables: ArrayMap<String, Productz>

    /**
     * Current cache of available subscriptions.
     */
    var subscriptions: ArrayMap<String, Productz>

    /**
     * kotlin coroutine function for querying a specific product
     */
    fun queryProduct(sku: String, type: Productz.Type): QueryResult<Productz>

    /**
     * @param products, a map of string productIds with a product type that will try to match
     * against (validate) the billing client's server for list of available products.
     */
    fun queryInventory(products: Map<String, Productz.Type>): QueryResult<Map<String, Productz>>

    /**
     * @param products
     * @param type
     * Mainly for internal use only.
     * Server verified skus will be sorted by type and cached in-memory.
     */
    fun updateInventory(products: List<Productz>?, type: Productz.Type)

    /**
     * Search for a specified product by sku id.
     * @return [Productz] - Android Billing Library object
     * @param sku - the sku product id
     */
    fun getProduct(sku: String?): Productz?

    /**
     * Get all available products (in cache),
     * set productType to null to query all products.
     * Available products are validated. Note: the list will be empty
     * if the inventory has not been properly updated.
     * @param type:
     * @param promo
     *
     */
    fun getProducts(type: Productz.Type?, promo: Productz.Promotion?): Map<String, Productz>

    /**
     * Validates if the inventory has collection of verified skus cached in-memory.
     */
    fun isReadyStateFlow(): StateFlow<Boolean>

    fun isReadyLiveData(): LiveData<Boolean>
}
