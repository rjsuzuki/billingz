package com.zuko.billingz.lib.products

import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.AcknowledgePurchaseResponseListener
import com.android.billingclient.api.Purchase
import com.zuko.billingz.BillingManager
import com.zuko.billingz.lib.model.PurchaseWrapper

import com.zuko.billingz.lib.client.Billing
import com.zuko.billingz.lib.sales.Order
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Subscriptions are handled similarly to non-consumables.
 * You can acknowledge a subscription Acknowledgement using either BillingClient.acknowledgePurchase()
 * from the Google Play Billing Library or Purchases.Subscriptions.Acknowledge from the Google Play Developer API.
 * All initial subscription purchases need to be acknowledged. Subscription renewals do not need to be
 * acknowledged. For more information on when subscriptions need to be acknowledged,
 * see the Sell subscriptions topic.
 */
class Subscriptions: Product {

    override val type: Product.ProductType = Product.ProductType.SUBSCRIPTION



    companion object {
        private const val TAG = "Subscriptions"
    }
}