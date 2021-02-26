package com.zuko.billingz.lib.products

import androidx.lifecycle.MutableLiveData
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.AcknowledgePurchaseResponseListener
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase
import com.zuko.billingz.lib.sales.Order
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Subscriptions are handled similarly to non-consumables.
 * You can acknowledge a subscription Acknowledgement using either BillingClient.acknowledgePurchase()
 * from the Google Play Billing Library or Purchases.Subscriptions.Acknowledge from the Google Play Developer API.
 * All initial subscription purchases need to be acknowledged. Subscription renewals do not need to be
 * acknowledged. For more information on when subscriptions need to be acknowledged,
 * see the Sell subscriptions topic.
 */
object Subscription: Product {
    private const val TAG = "Subscriptions"
    override val type: Product.ProductType = Product.ProductType.SUBSCRIPTION
    override fun completeOrder(
        billingClient: BillingClient?,
        purchase: Purchase,
        order: MutableLiveData<Order>,
        mainScope: CoroutineScope?
    ) {
        val listener = AcknowledgePurchaseResponseListener { billingResult ->
            val data = Order(
                purchase = purchase,
                billingResult = billingResult,
                msg = "Subscription successfully purchased"
            )
            order.postValue(data)
        }

        if(purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams =
                    AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken)

                mainScope?.launch(Dispatchers.IO) {
                    billingClient
                        ?.acknowledgePurchase(acknowledgePurchaseParams.build(), listener)
                }
            }
        }
    }


}