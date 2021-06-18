package com.zuko.billingz.amazon.store.inventory

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.zuko.billingz.lib.store.inventory.Inventory
import com.zuko.billingz.lib.store.products.Product

class AmazonInventory: Inventory {

    override var allProducts: Map<String, Product> = HashMap()
    override var consumables: Map<String, Product> = HashMap()
    override var nonConsumables: Map<String, Product> = HashMap()
    override var subscriptions: Map<String, Product> = HashMap()
    override var requestedProducts: MutableLiveData<Map<String, Product>> = MutableLiveData()

    override fun loadProducts(skuList: MutableList<String>, type: Product.Type) {
        TODO("Not yet implemented")
    }

    override fun loadPromotions(skuList: MutableList<String>, promo: Product.Promotion) {
        TODO("Not yet implemented")
    }

    override fun queryInventory(skuList: MutableList<String>, productType: Product.Type) {
        TODO("Not yet implemented")
    }

    override fun updateInventory(products: MutableList<Product>?, productType: Product.Type) {
        TODO("Not yet implemented")
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

    override fun destroy() {
        TODO("Not yet implemented")
    }
}