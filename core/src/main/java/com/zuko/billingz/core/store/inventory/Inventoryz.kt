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
package com.zuko.billingz.core.store.inventory

import android.util.ArrayMap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.zuko.billingz.core.misc.CleanUpz
import com.zuko.billingz.core.store.model.Productz

/**
 * Blueprint for managing a store's (your applications) entire collection of available products (inventory)
 */
interface Inventoryz : CleanUpz {


    /**
     * Current cache of all products that is provided by your app/server.
     */
    var allProducts: Map<String, Productz.Type>

    /**
     * Current cache of available consumables.
     */
    var consumables: Map<String, Productz>

    /**
     * Current cache of available non-consumables.
     */
    var nonConsumables: Map<String, Productz>

    /**
     * Current cache of available subscriptions.
     */
    var subscriptions: Map<String, Productz>

    /**
     * Provides the most recent collection of Products queried.
     * May or may not be filtered by product type.
     */
    var requestedProducts: MutableLiveData<Map<String, Productz>>

    /**
     *
     * @param skuList, a list of string productIds that will try to match
     * against (validate) the billing client's server for list of available products.
     * @param type
     */
    fun queryInventory(products: Map<String, Productz.Type>): LiveData<Map<String, Productz>>

    /**
     * @param products
     * @param type
     * Mainly for internal use only
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

}
