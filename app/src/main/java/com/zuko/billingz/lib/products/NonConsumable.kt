package com.zuko.billingz.lib.products

import com.android.billingclient.api.Purchase
import com.android.billingclient.api.SkuDetails

interface NonConsumable: Product  {
    var nonConsumables: Map<String, SkuDetails>
    fun processNonConsumable(purchase: Purchase)
    fun loadNonConsumableProducts(skuList: MutableList<String>)
}