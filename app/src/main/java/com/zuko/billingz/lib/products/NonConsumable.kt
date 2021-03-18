package com.zuko.billingz.lib.products

import androidx.lifecycle.MutableLiveData
import com.android.billingclient.api.*
import com.zuko.billingz.lib.sales.Order
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 *
 * @author rjsuzuki
 */
data class NonConsumable(
    override var id: Int? = 0,
    override var sku: String? = null,
    override var name: String? = null,
    override var price: String? = null,
    override var description: String? = null,
    override var details: SkuDetails? = null
): Product {

    override val skuType: String = BillingClient.SkuType.INAPP
    override val type: Product.ProductType = Product.ProductType.NON_CONSUMABLE

    companion object {
        private const val TAG = "NonConsumable"

        fun completeOrder(
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
}