package com.zuko.billingz.lib.products

import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.AcknowledgePurchaseResponseListener
import com.android.billingclient.api.Purchase
import com.zuko.billingz.lib.model.PurchaseWrapper
import com.zuko.billingz.lib.client.Billing
import com.zuko.billingz.lib.sales.Order
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class NonConsumables: Product {

    override val type: Product.ProductType = Product.ProductType.NON_CONSUMABLE

    override fun completeOrder(client: Billing, purchase: Purchase, scope: CoroutineScope) {

    }

    companion object {
        private const val TAG = "NonConsumable"
    }
}