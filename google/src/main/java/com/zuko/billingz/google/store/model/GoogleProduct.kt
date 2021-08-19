package com.zuko.billingz.google.store.model

import com.android.billingclient.api.SkuDetails
import com.zuko.billingz.core.store.model.Productz

data class GoogleProduct(val skuDetails: SkuDetails): Productz {

    override var id: Int? = null
    override var sku: String? = null
    override var name: String? = null
    override var price: String? = null
    override var description: String? = null
    override var iconUrl: String? = null
    override val type: Productz.Type = Productz.Type.UNKNOWN
    override val promotion: Productz.Promotion = Productz.Promotion.NONE
}