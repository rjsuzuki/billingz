package com.zuko.billingz.lib.products

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.Purchase
import com.zuko.billingz.lib.LogUtil

import com.zuko.billingz.lib.sales.Order
import kotlinx.coroutines.CoroutineScope

/**
 * @author rjsuzuki
 */
object Consumable: Product {

    private const val TAG = "Consumable"
    override val type: Product.ProductType = Product.ProductType.CONSUMABLE

    override fun completeOrder(billingClient: BillingClient?,
                               purchase: Purchase,
                               order: MutableLiveData<Order>,
                               mainScope: CoroutineScope?) {

        val consumeParams = ConsumeParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient?.consumeAsync(consumeParams) { billingResult, p ->
            val msg: String
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                msg = "Consumable successfully purchased: $p"
                Log.d(TAG, "Product successfully purchased: ${purchase.sku}")
            } else {
                msg = billingResult.debugMessage
                LogUtil.log.e(TAG, "Error purchasing consumable. $msg")
            }
            val data = Order(
                purchase = purchase,
                billingResult = billingResult,
                msg = msg
            )
            order.postValue(data)
        }
    }
}