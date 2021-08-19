package com.zuko.billingz.amazon.store.model

import com.zuko.billingz.core.store.model.Productz

data class AmazonProduct(var amazonProduct: com.amazon.device.iap.model.Product): Productz {
    override var id: Int? = -1
    override var sku: String? = null
    override var name: String? = null
    override var price: String? = null
    override var description: String? = null
    override var iconUrl: String? = null
    override val type: Productz.Type = Productz.Type.UNKNOWN
    override val promotion: Productz.Promotion = Productz.Promotion.NONE
}