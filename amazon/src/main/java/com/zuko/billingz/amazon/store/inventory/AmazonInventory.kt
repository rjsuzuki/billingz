package com.zuko.billingz.amazon.store.inventory

import android.util.ArrayMap
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.amazon.device.iap.PurchasingService
import com.zuko.billingz.core.LogUtilz
import com.zuko.billingz.core.store.inventory.Inventoryz
import com.zuko.billingz.core.store.model.Productz

class AmazonInventory: Inventoryz {

    override var allProducts: Map<String, Productz.Type> = HashMap()
    override var consumables: Map<String, Productz> = HashMap()
    override var nonConsumables: Map<String, Productz> = HashMap()
    override var subscriptions: Map<String, Productz> = HashMap()
    override var requestedProducts: MutableLiveData<Map<String, Productz>> = MutableLiveData()

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
            when (type) {
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
                else -> LogUtilz.log.w(TAG, "Unhandled product type: $type")
            }
        }
    }

    fun getAvailableProducts(
        skuList: MutableList<String>,
        productType: Productz.Type?
    ): LiveData<Map<String, Productz>> {

        when(productType) {
            Productz.Type.CONSUMABLE -> {
                //consumables = consumables + products.associateBy { it.sku.toString() }
                requestedProducts.postValue(consumables)
            }
            Productz.Type.NON_CONSUMABLE -> {
                //nonConsumables = nonConsumables + products.associateBy { it.sku.toString() }
                requestedProducts.postValue(nonConsumables)
            }
            Productz.Type.SUBSCRIPTION -> {
                //subscriptions = subscriptions + products.associateBy { it.sku.toString() }
                requestedProducts.postValue(subscriptions)
            }
        }
        return requestedProducts
    }

    override fun getProduct(sku: String?): Productz? {
        if(consumables.containsKey(sku))
            return consumables[sku]
        if(nonConsumables.containsKey(sku))
            return nonConsumables[sku]
        if(subscriptions.containsKey(sku))
            return subscriptions[sku]
        return null
    }

    override fun getProducts(type: Productz.Type?, promo: Productz.Promotion?): Map<String, Productz> {
        when (type) {
            Productz.Type.CONSUMABLE -> {
                if(promo != null) {
                    consumables.forEach { entry ->
                        val promos = ArrayMap<String, Productz>()
                        if(entry.value.promotion == promo) {
                            promos[entry.key] = entry.value
                        }
                        return promos
                    }
                }
                return consumables
            }
            Productz.Type.NON_CONSUMABLE -> {
                if(promo != null) {
                    nonConsumables.forEach { entry ->
                        val promos = ArrayMap<String, Productz>()
                        if(entry.value.promotion == promo) {
                            promos[entry.key] = entry.value
                        }
                        return promos
                    }
                }
                return nonConsumables
            }
            Productz.Type.SUBSCRIPTION -> {
                if(promo != null ) {
                    subscriptions.forEach { entry ->
                        val promos = ArrayMap<String, Productz>()
                        if(entry.value.promotion == promo) {
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
    }

    companion object {
        private const val TAG = "AmazonInventory"
    }
}