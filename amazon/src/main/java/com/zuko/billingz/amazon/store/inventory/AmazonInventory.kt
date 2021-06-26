package com.zuko.billingz.amazon.store.inventory

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.amazon.device.iap.PurchasingService
import com.zuko.billingz.lib.LogUtil
import com.zuko.billingz.lib.store.inventory.Inventory
import com.zuko.billingz.lib.store.model.Product

class AmazonInventory: Inventory {
    override var consumableSkus: MutableList<String> = mutableListOf()
    override var nonConsumableSkus: MutableList<String> = mutableListOf()
    override var subscriptionSkus: MutableList<String> = mutableListOf()

    override var allProducts: Map<String, Product> = HashMap()
    override var consumables: Map<String, Product> = HashMap()
    override var nonConsumables: Map<String, Product> = HashMap()
    override var subscriptions: Map<String, Product> = HashMap()
    override var requestedProducts: MutableLiveData<Map<String, Product>> = MutableLiveData()

    override fun queryInventory(skuList: List<String>, productType: Product.Type) {
        // Call this method to retrieve item data for a set of SKUs to display in your app.
        // Call getProductData in the OnResume method.
        val productDataRequestId = PurchasingService.getProductData(skuList.toSet()) // inventory
        Log.v(TAG, "get product data request: $productDataRequestId")
    }

    override fun updateInventory(products: List<Product>?, productType: Product.Type) {
        Log.d(TAG, "updateInventory : ${products?.size ?: 0}")
        if (!products.isNullOrEmpty()) {
            allProducts = allProducts + products.associateBy { it.sku.toString() }

            when (productType) {
                Product.Type.CONSUMABLE -> {
                    consumables = consumables + products.associateBy { it.sku.toString() }
                    requestedProducts.postValue(consumables)
                }
                Product.Type.NON_CONSUMABLE -> {
                    nonConsumables = nonConsumables + products.associateBy { it.sku.toString() }
                    requestedProducts.postValue(nonConsumables)
                }
                Product.Type.SUBSCRIPTION -> {
                    subscriptions = subscriptions + products.associateBy { it.sku.toString() }
                    requestedProducts.postValue(subscriptions)
                }
                else -> LogUtil.log.w(TAG, "Unhandled product type: $productType")
            }
        }
    }

    override fun getAvailableProducts(
        skuList: MutableList<String>,
        productType: Product.Type?
    ): LiveData<Map<String, Product>> {

        when(productType) {
            Product.Type.CONSUMABLE -> {
                //consumables = consumables + products.associateBy { it.sku.toString() }
                requestedProducts.postValue(consumables)
            }
            Product.Type.NON_CONSUMABLE -> {
                //nonConsumables = nonConsumables + products.associateBy { it.sku.toString() }
                requestedProducts.postValue(nonConsumables)
            }
            Product.Type.SUBSCRIPTION -> {
                //subscriptions = subscriptions + products.associateBy { it.sku.toString() }
                requestedProducts.postValue(subscriptions)
            }
        }
        return requestedProducts
    }

    override fun getProduct(sku: String): Product? {
        return allProducts[sku]
    }

    override fun getProducts(type: Product.Type?, promo: Product.Promotion?): List<Product> {
        when (type) {
            Product.Type.CONSUMABLE -> {
                if(promo != null) {
                    consumables.values.iterator().forEach { product ->
                        val promos = mutableListOf<Product>()
                        if(product.promotion == promo) {
                            promos.add(product)
                        }
                        return promos
                    }
                }
                return consumables.values.toList()
            }
            Product.Type.NON_CONSUMABLE -> {
                if(promo != null) {
                    nonConsumables.values.iterator().forEach { product ->
                        val promos = mutableListOf<Product>()
                        if(product.promotion == promo) {
                            promos.add(product)
                        }
                        return promos
                    }
                }
                return nonConsumables.values.toList()
            }
            Product.Type.SUBSCRIPTION -> {
                if(promo != null) {
                    subscriptions.values.iterator().forEach { product ->
                        val promos = mutableListOf<Product>()
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

    override fun destroy() {
        LogUtil.log.v(TAG, "destroy")
    }

    companion object {
        private const val TAG = "AmazonInventory"
    }
}