package com.zuko.billingz.amazon.store.model

import com.amazon.device.iap.model.ProductType
import com.zuko.billingz.core.store.model.Productz
import java.util.Currency
import java.util.Locale

data class AmazonProduct(var amazonProduct: com.amazon.device.iap.model.Product): Productz {
    override var id: Int = -1
    override var sku: String? = amazonProduct.sku
    override var title: String? = amazonProduct.title
    override var price: String? = amazonProduct.price
    override var description: String? = amazonProduct.description
    override var iconUrl: String? = amazonProduct.smallIconUrl
    override val type: Productz.Type = when(amazonProduct.productType) {
        ProductType.CONSUMABLE -> Productz.Type.CONSUMABLE
        ProductType.ENTITLED -> Productz.Type.NON_CONSUMABLE
        ProductType.SUBSCRIPTION -> Productz.Type.SUBSCRIPTION
        else -> Productz.Type.UNKNOWN
    }
    override val promotion: Productz.Promotion = Productz.Promotion.NONE

    override fun getCurrency(): Currency {
        return Currency.getInstance(Locale.getDefault())
    }
}