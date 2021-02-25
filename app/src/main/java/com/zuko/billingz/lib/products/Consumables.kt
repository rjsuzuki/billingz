package com.zuko.billingz.lib.products

import android.util.Log
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.Purchase
import com.zuko.billingz.lib.model.PurchaseWrapper
import com.zuko.billingz.lib.client.Billing
import com.zuko.billingz.lib.sales.Order
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

class Consumables: Product {

    override val type: Product.ProductType = Product.ProductType.CONSUMABLE

    companion object {
        private const val TAG = "Consumable"
    }
}