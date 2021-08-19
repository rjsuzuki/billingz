package com.zuko.billingz.google.store.inventory

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.SkuDetailsParams
import com.zuko.billingz.google.store.client.GoogleClient
import com.zuko.billingz.google.store.model.GoogleProduct
import com.zuko.billingz.core.LogUtilz
import com.zuko.billingz.core.store.inventory.Inventoryz
import com.zuko.billingz.core.store.model.Productz

class GoogleInventory(private val client: GoogleClient): Inventoryz {

    override var consumableSkus: MutableList<String> = mutableListOf()
    override var nonConsumableSkus: MutableList<String> = mutableListOf()
    override var subscriptionSkus: MutableList<String> = mutableListOf()

    override var allProducts: Map<String, Productz> = HashMap()
    override var consumables: Map<String, Productz> = HashMap()
    override var nonConsumables: Map<String, Productz> = HashMap()
    override var subscriptions: Map<String, Productz> = HashMap()
    
    override var requestedProducts: MutableLiveData<Map<String, Productz>> = MutableLiveData()

    override fun queryInventory(skuList: List<String>, productType: Productz.Type) {
        val type = when(productType) {
            Productz.Type.CONSUMABLE -> BillingClient.SkuType.INAPP
            Productz.Type.NON_CONSUMABLE -> BillingClient.SkuType.INAPP
            Productz.Type.SUBSCRIPTION -> BillingClient.SkuType.SUBS
            else -> {
                BillingClient.SkuType.INAPP
            }
        }
        val builder = SkuDetailsParams.newBuilder()
        val params = builder
            .setSkusList(skuList)
            .setType(type)
            .build()
        client.getBillingClient()?.querySkuDetailsAsync(params) { result, skuDetailsList ->
            Log.i(TAG, "Processing query result : ${result.responseCode}, ${result.debugMessage}")

            val list = mutableListOf<Productz>()
            skuDetailsList?.let { skus ->
                for(s in skus) {
                    val product = GoogleProduct(s)
                    list.add(product)
                }
            }
            updateInventory(list, productType)
        }
    }

    override fun updateInventory(products: List<Productz>?, productType: Productz.Type) {
        Log.d(TAG, "updateInventory : ${products?.size ?: 0}")
        if (!products.isNullOrEmpty()) {
            allProducts = allProducts + products.associateBy { it.sku.toString() }

            when (productType) {
                Productz.Type.CONSUMABLE -> {
                    consumables = consumables + products.associateBy { it.sku.toString() }
                    requestedProducts.postValue(consumables)
                }
                Productz.Type.NON_CONSUMABLE -> {
                    nonConsumables = nonConsumables + products.associateBy { it.sku.toString() }
                    requestedProducts.postValue(nonConsumables)
                }
                Productz.Type.SUBSCRIPTION -> {
                    subscriptions = subscriptions + products.associateBy { it.sku.toString() }
                    requestedProducts.postValue(subscriptions)
                }
                else -> LogUtilz.log.w(TAG, "Unhandled product type: $productType")
            }
        }
    }

    override fun getAvailableProducts(
        skuList: MutableList<String>,
        productType: Productz.Type?
    ): LiveData<Map<String, Productz>> {
        return requestedProducts
    }

    override fun getProduct(sku: String): Productz? {
        return allProducts[sku]
    }

    override fun getProducts(type: Productz.Type?, promo: Productz.Promotion?): List<Productz> {
        when (type) {
            Productz.Type.CONSUMABLE -> {
                if(promo != null) {
                    consumables.values.iterator().forEach { product ->
                        val promos = mutableListOf<Productz>()
                        if(product.promotion == promo) {
                            promos.add(product)
                        }
                        return promos
                    }
                }
                return consumables.values.toList()
            }
            Productz.Type.NON_CONSUMABLE -> {
                if(promo != null) {
                    nonConsumables.values.iterator().forEach { product ->
                        val promos = mutableListOf<Productz>()
                        if(product.promotion == promo) {
                            promos.add(product)
                        }
                        return promos
                    }
                }
                return nonConsumables.values.toList()
            }
            Productz.Type.SUBSCRIPTION -> {
                if(promo != null) {
                    subscriptions.values.iterator().forEach { product ->
                        val promos = mutableListOf<Productz>()
                        if(product.promotion == promo) {
                            promos.add(product)
                        }
                        return promos
                    }
                }
                return subscriptions.values.toList()
            }
            else -> return allProducts.values.toList()
        }
    }

    fun isConsumable(purchase: Purchase): Boolean {
        return !consumables.isNullOrEmpty() && consumables.contains(purchase.orderId)
    }

    override fun destroy() {
        Log.v(TAG,"destroy")
    }

    companion object {
        private const val TAG = "GoogleInventory"
    }
}