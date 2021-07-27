package com.zuko.billingz.google.store.model

import com.android.billingclient.api.SkuDetails
import com.zuko.billingz.lib.store.model.Product

data class GoogleProduct(val skuDetails: SkuDetails): Product {

    override var id: Int? = null
    override var sku: String? = null
    override var name: String? = null
    override var price: String? = null
    override var description: String? = null
    override var iconUrl: String? = null
    override val type: Product.Type = Product.Type.UNKNOWN
    override val promotion: Product.Promotion = Product.Promotion.NONE
}