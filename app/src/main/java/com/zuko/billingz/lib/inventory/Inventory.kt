package com.zuko.billingz.lib.inventory

import androidx.lifecycle.MutableLiveData
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.SkuDetails
import com.zuko.billingz.lib.extra.CleanUp
import com.zuko.billingz.lib.products.Product


interface Inventory : CleanUp {
    var requestedProducts: MutableLiveData<Map<String, SkuDetails>>
    fun isConsumable(purchase: Purchase) : Boolean

    fun getProductDetails(productId: String?) : SkuDetails?
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

    fun loadFreeProducts(skuList: MutableList<String>, productType: Product.ProductType)

    fun loadPromotions(skuList: MutableList<String>, productType: Product.ProductType)

    fun querySkuDetails(skuList: MutableList<String>, productType: Product.ProductType)

    fun updateSkuDetails(skuDetailsList: List<SkuDetails>?, productType: Product.ProductType)

}