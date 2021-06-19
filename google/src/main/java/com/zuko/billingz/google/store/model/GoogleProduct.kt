package com.zuko.billingz.google.store.model

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.SkuDetails
import com.zuko.billingz.lib.store.model.Product

data class GoogleProduct(val skuDetails: SkuDetails): Product {

    override var id: Int?
        get() = TODO("Not yet implemented")
        set(value) {}
    override var sku: String?
        get() = TODO("Not yet implemented")
        set(value) {}
    override var name: String?
        get() = TODO("Not yet implemented")
        set(value) {}
    override var price: String?
        get() = TODO("Not yet implemented")
        set(value) {}
    override var description: String?
        get() = description
        set(value) {}
    override var iconUrl: String?
        get() = TODO("Not yet implemented")
        set(value) {}
    override val type: Product.Type
        get() = TODO("Not yet implemented")
    override val promotion: Product.Promotion
        get() = TODO("Not yet implemented")
}