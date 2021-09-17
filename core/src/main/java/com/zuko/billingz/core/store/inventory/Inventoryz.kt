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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.zuko.billingz.core.misc.CleanUpz
import com.zuko.billingz.core.store.model.Productz

/**
 * Blueprint for managing a store's (your applications) entire collection of available products (inventory)
 */
interface Inventoryz : CleanUpz {

    var consumableSkus: MutableList<String>
    var nonConsumableSkus: MutableList<String>
    var subscriptionSkus: MutableList<String>

    /**
     *
     */
    var allProducts: Map<String, Productz>

    /**
     *
     */
    var consumables: Map<String, Productz>

    /**
     *
     */
    var nonConsumables: Map<String, Productz>

    /**
     *
     */
    var subscriptions: Map<String, Productz>

    /**
     *
     */
    var requestedProducts: MutableLiveData<Map<String, Productz>>

    /**
     *
     * @param skuList, a list of string productIds that will try to match
     * against (validate) the billing client's server for list of available products.
     * @param productType
     */
    fun queryInventory(skuList: List<String>, productType: Productz.Type)

    /**
     * @param products
     * @param productType
     */
    fun updateInventory(products: List<Productz>?, productType: Productz.Type)

    /**
     * Get the details for a specified product.
     * @return [Productz] - Android Billing Library object
     * @param sku - the product id that can be found on the GooglePlayConsole
     */
    fun getProduct(sku: String): Productz?

    fun getProducts(type: Productz.Type?, promo: Productz.Promotion?): List<Productz>

    /**
     * Get all available products,
     * set productType to null to query all products.
     * Available products have been validated.
     * @param skuList: MutableList<String>
     * @param productType: Product.ProductType
     * @return [LiveData<Map<String, Product>]
     */
    fun getAvailableProducts(
        skuList: MutableList<String>,
        productType: Productz.Type?
    ): LiveData<Map<String, Productz>>
}