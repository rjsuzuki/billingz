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

object NonConsumable: Product {
    private const val TAG = "NonConsumable"
    override val type: Product.ProductType = Product.ProductType.NON_CONSUMABLE


    override fun completeOrder(
        billingClient: BillingClient?,
        purchase: Purchase,
        order: MutableLiveData<Order>,
        mainScope: CoroutineScope?
    ) {
        val listener = AcknowledgePurchaseResponseListener { billingResult ->
            if(billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val data = Order(
                    purchase = purchase,
                    billingResult = billingResult,
                    msg = "Non-Consumable successfully purchased"
                )
                order.postValue(data)
            }
        }

        if(purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if(!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken)
                mainScope?.launch(Dispatchers.IO) {
                    billingClient?.acknowledgePurchase(acknowledgePurchaseParams.build(), listener)
                }
            }
        }
    }
}