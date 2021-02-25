package com.zuko.billingz.lib.inventory

import androidx.annotation.UiThread
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.SkuDetails
import com.zuko.billingz.lib.model.CleanUp
import com.zuko.billingz.lib.products.Product


interface Search : CleanUp {

   /* fun getAllProducts(): Map<String, SkuDetails>
    fun getConsumables(): Map<String, SkuDetails>
    fun getNonConsumables(): Map<String, SkuDetails>
    fun getSubscriptions(): Map<String, SkuDetails>*/

    fun isConsumable(purchase: Purchase) : Boolean

    fun getProductDetails(productId: String?) : SkuDetails?
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
    fun loadSubscriptionProducts(skuList: MutableList<String>)

    fun loadFreeProducts(skuList: MutableList<String>, productType: Product.ProductType)

    fun loadPromotions(skuList: MutableList<String>, productType: Product.ProductType)

    fun querySkuDetails(skuList: MutableList<String>, productType: Product.ProductType)

    fun updateSkuDetails(skuDetailsList: List<SkuDetails>?, productType: Product.ProductType)

}