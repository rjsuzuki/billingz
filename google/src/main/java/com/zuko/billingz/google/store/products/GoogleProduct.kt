package com.zuko.billingz.google.store.products

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.SkuDetails
import com.zuko.billingz.lib.store.products.Product

interface GoogleProduct: Product {

    /**
     * @see [BillingClient.SkuType]
     */
    val skuType: String

    /**
     * @see [SkuDetails]
     */
    var details: SkuDetails?


}