package com.zuko.billingz.google.store.inventory

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.SkuDetailsParams
import com.zuko.billingz.google.store.client.GoogleClient
import com.zuko.billingz.lib.LogUtil
import com.zuko.billingz.lib.store.inventory.Inventory
import com.zuko.billingz.lib.store.products.Product

class GoogleInventory(private val client: GoogleClient): Inventory {

    override var allProducts: Map<String, Product> = HashMap()
    override var consumables: Map<String, Product> = HashMap()
    override var nonConsumables: Map<String, Product> = HashMap()
    override var subscriptions: Map<String, Product> = HashMap()
    override var requestedProducts: MutableLiveData<Map<String, Product>> = MutableLiveData()

    override fun loadProducts(skuList: MutableList<String>, productType: Product.Type) {
        when(productType) {
            Product.Type.CONSUMABLE -> {

            }
            Product.Type.NON_CONSUMABLE -> {

            }
            Product.Type.SUBSCRIPTION -> {

            }
        }
    }

    override fun loadPromotions(skuList: MutableList<String>, promo: Product.Promotion) {
        TODO("Not yet implemented")
    }

    override fun queryInventory(skuList: MutableList<String>, productType: Product.Type) {
        val type = when(productType) {
            Product.Type.CONSUMABLE -> BillingClient.SkuType.INAPP
            Product.Type.NON_CONSUMABLE -> BillingClient.SkuType.INAPP
            Product.Type.SUBSCRIPTION -> BillingClient.SkuType.SUBS
        }
        val builder = SkuDetailsParams.newBuilder()
        val params = builder
            .setSkusList(skuList)
            .setType(type)
            .build()
        client.getBillingClient()?.querySkuDetailsAsync(params) { result, skuDetailsList ->
            Log.i(TAG, "Processing query result : ${result.responseCode}, ${result.debugMessage}")
            updateInventory(skuDetailsList, productType)
        }
    }

    override fun updateInventory(products: MutableList<Product>?, productType: Product.Type) {
        Log.d(TAG, "updateSkuDetails : ${products?.size ?: 0}")
        if (!products.isNullOrEmpty()) {
            allProducts = allProducts + products.associateBy { it.sku }

            when (productType) {
                Product.Type.CONSUMABLE -> {
                    consumables = consumables + products.associateBy { it.sku }
                    requestedProducts.postValue(consumables)
                }
                Product.Type.NON_CONSUMABLE -> {
                    nonConsumables = nonConsumables + products.associateBy { it.sku }
                    requestedProducts.postValue(nonConsumables)
                }
                Product.Type.SUBSCRIPTION -> {
                    subscriptions = subscriptions + products.associateBy { it.sku }
                    requestedProducts.postValue(subscriptions)
                }
                else -> LogUtil.log.w(TAG, "Unhandled product type: $productType")
            }
        }
    }

    override fun getProduct(sku: String): Product? {
        TODO("Not yet implemented")
    }

    override fun getAvailableProducts(
        skuList: MutableList<String>,
        productType: Product.Type
    ): LiveData<Map<String, Product>> {
        TODO("Not yet implemented")
    }

    fun isConsumable(purchase: Purchase): Boolean {
        return !consumables.isNullOrEmpty() && consumables.contains(purchase.sku)
    }

    override fun destroy() {
        Log.v(TAG,"destroy")
    }

    companion object {
        private const val TAG = "BillingzInventory"
    }
}