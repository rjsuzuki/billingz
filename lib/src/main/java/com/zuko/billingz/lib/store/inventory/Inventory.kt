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
package com.zuko.billingz.lib.store.inventory

import androidx.lifecycle.MutableLiveData
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.SkuDetails
import com.zuko.billingz.lib.misc.CleanUpListener
import com.zuko.billingz.lib.store.products.Product

/**
 * Blueprint for managing a store's (your applications) entire collection of available products (inventory)
 */
interface Inventory : CleanUpListener {

    /**
     *
     */
    var consumables: Map<String, SkuDetails>

    /**
     *
     */
    var nonConsumables: Map<String, SkuDetails>

    /**
     *
     */
    var subscriptions: Map<String, SkuDetails>

    /**
     *
     */
    var requestedProducts: MutableLiveData<Map<String, SkuDetails>>

    /**
     *
     */
    fun isConsumable(purchase: Purchase): Boolean

    /**
     *
     */
    fun getProductDetails(productId: String?): SkuDetails?

    /**
     *
     */
    var allProducts: Map<String, SkuDetails>

    /**
     * @param isConsumables - indicate whether the skuList is for consumables or not. (Do not mix consumables
     * and non-consumables in same list if possible)
     * @param skuList, a list of string productIds that will try to match
     * against Google Play's list of available subscriptions
     */
    fun loadInAppProducts(skuList: MutableList<String>, isConsumables: Boolean)

    /**
     * @param skuList, a list of string productIds that will try to match
     * against Google Play's list of available subscriptions
     */
    fun loadSubscriptions(skuList: MutableList<String>)

    /**
     * @param skuList
     * @param productType
     */
    fun loadFreeProducts(skuList: MutableList<String>, productType: Product.ProductType)

    /**
     * @param skuList
     * @param productType
     */
    fun loadPromotions(skuList: MutableList<String>, productType: Product.ProductType)

    /**
     * @param skuList
     * @param productType
     */
    fun querySkuDetails(skuList: MutableList<String>, productType: Product.ProductType)

    /**
     * @param skuList
     * @param productType
     */
    fun updateSkuDetails(skuDetailsList: List<SkuDetails>?, productType: Product.ProductType)
}
