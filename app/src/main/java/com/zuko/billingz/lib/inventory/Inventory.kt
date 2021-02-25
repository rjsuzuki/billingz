package com.zuko.billingz.lib.inventory

import android.util.Log
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.SkuDetails
import com.android.billingclient.api.SkuDetailsParams
import com.zuko.billingz.BillingManager
import com.zuko.billingz.lib.LogUtil
import com.zuko.billingz.lib.client.Billing
import com.zuko.billingz.lib.products.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class Inventory(val billing: Billing) : Search {

    var subscription: Product = Subscriptions()
    var consumable: Product = Consumables()
    var nonConsumable: Product = NonConsumables()

    var promotions: Map<String, SkuDetails> = HashMap()
    var freeProducts: Map<String, SkuDetails> = HashMap()
    var consumables: Map<String, SkuDetails> = HashMap()
    var nonConsumables: Map<String, SkuDetails> = HashMap()
    var subscriptions: Map<String, SkuDetails> = HashMap()
    var allProducts: Map<String, SkuDetails> = HashMap()

    override fun getProductDetails(productId: String?) : SkuDetails? {
        productId?.let {
            return allProducts[productId]
        } ?: return null
    }

    override fun loadInAppProducts(skuList: MutableList<String>, isConsumables: Boolean) {
        val type = if(isConsumables) Product.ProductType.CONSUMABLE else Product.ProductType.NON_CONSUMABLE
        querySkuDetails(skuList, type)
    }

    override fun loadSubscriptionProducts(skuList: MutableList<String>) {
        querySkuDetails(skuList, Product.ProductType.SUBSCRIPTION)
    }

    override fun loadFreeProducts(skuList: MutableList<String>, productType: Product.ProductType) {
        querySkuDetails(skuList, productType)
    }

    override fun loadPromotions(skuList: MutableList<String>, productType: Product.ProductType) {
        querySkuDetails(skuList, productType)
    }

    override fun querySkuDetails(skuList: MutableList<String>, productType: Product.ProductType) {
        Log.v(TAG, "Sku details: product type : $productType, list: ${skuList.size}")
        val type = when(productType) {
            Product.ProductType.SUBSCRIPTION -> BillingClient.SkuType.SUBS
            Product.ProductType.CONSUMABLE -> BillingClient.SkuType.INAPP
            Product.ProductType.NON_CONSUMABLE -> BillingClient.SkuType.INAPP

            Product.ProductType.FREE_SUBSCRIPTION -> BillingClient.SkuType.SUBS
            Product.ProductType.FREE_CONSUMABLE -> BillingClient.SkuType.INAPP
            Product.ProductType.FREE_NON_CONSUMABLE -> BillingClient.SkuType.INAPP

            Product.ProductType.PROMO_SUBSCRIPTION -> BillingClient.SkuType.SUBS
            Product.ProductType.PROMO_CONSUMABLE -> BillingClient.SkuType.INAPP
            Product.ProductType.PROMO_NON_CONSUMABLE -> BillingClient.SkuType.INAPP
            else -> {
                LogUtil.log.w(TAG, "Unknown productType: $productType received, defaulting to SkuType.INAPP")
                BillingClient.SkuType.INAPP
            }
        }
        val builder = SkuDetailsParams.newBuilder()
        val params = builder
            .setSkusList(skuList)
            .setType(type)
            .build()
        billing.getBillingClient()?.querySkuDetailsAsync(params) { result, skuDetailsList ->
            Log.i(TAG, "Processing query result : ${result.responseCode}, ${result.debugMessage}")
            updateSkuDetails(skuDetailsList, productType)
        }
    }

    //update list
    @Synchronized
    override fun updateSkuDetails(skuDetailsList: List<SkuDetails>?, productType: Product.ProductType) {
        Log.d(TAG, "updateSkuDetails : ${skuDetailsList?.size ?: 0}")
        if (!skuDetailsList.isNullOrEmpty()) {
            allProducts = allProducts + skuDetailsList.associateBy { it.sku }

            when(productType) {
                Product.ProductType.CONSUMABLE -> {
                    consumables = consumables + skuDetailsList.associateBy { it.sku }
                }
                Product.ProductType.NON_CONSUMABLE -> {
                    nonConsumables = nonConsumables + skuDetailsList.associateBy { it.sku }
                }
                Product.ProductType.SUBSCRIPTION -> {
                    subscriptions = subscriptions + skuDetailsList.associateBy { it.sku }
                }
                else -> LogUtil.log.w(TAG, "Unhandled product type: $productType")
            }
        }
    }

    override fun isConsumable(purchase: Purchase): Boolean {
        return !consumables.isNullOrEmpty() && consumables.contains(purchase.sku)
    }

    companion object {
        private const val TAG = "Inventory"
    }
}