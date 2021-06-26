package com.zuko.billingz.amazon.store.model

import com.zuko.billingz.lib.store.model.Product

data class AmazonProduct(var amazonProduct: com.amazon.device.iap.model.Product): Product {
    override var id: Int? = -1
    override var sku: String? = null
    override var name: String? = null
    override var price: String? = null
    override var description: String? = null
    override var iconUrl: String? = null
    override val type: Product.Type = Product.Type.UNKNOWN
    override val promotion: Product.Promotion = Product.Promotion.NONE
}