/*
 * Copyright 2021 rjsuzuki
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.zuko.billingz.google.store.sales

import android.app.Activity
import android.util.Log
import androidx.annotation.UiThread
import androidx.collection.ArrayMap
import androidx.lifecycle.MutableLiveData
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.SkuDetails
import com.zuko.billingz.lib.LogUtil
import com.zuko.billingz.lib.misc.BillingResponse
import com.zuko.billingz.lib.store.inventory.Inventory
import com.zuko.billingz.lib.store.products.Product
import com.zuko.billingz.lib.store.sales.Order

/**
 * Representation of the sales department of a store
 * @constructor
 * @param inventory
 */
class ProductSales(private val inventory: Inventory) : Sales {

    private var isQueriedOrders = false
    override var order = MutableLiveData<Order>()
    override var queriedOrder = MutableLiveData<Order>()
    override var orderUpdateListener: Sales.OrderUpdateListener? = null
    override var orderValidatorListener: Sales.OrderValidatorListener? = null
    override var purchasesUpdatedListener: PurchasesUpdatedListener =
        PurchasesUpdatedListener { billingResult, purchases -> processUpdatedPurchases(billingResult, purchases) }

    override var pendingPurchases = ArrayMap<String, Purchase>()

    private val validation: Sales.ValidatorCallback = object : Sales.ValidatorCallback {
        override fun onSuccess(purchase: Purchase) {
            processPurchase(purchase)
        }

        override fun onFailure(purchase: Purchase) {
            processPurchasingError(null)
        }
    }

    override fun getOrderOrQueried(): MutableLiveData<Order> {
        return if (isQueriedOrders) queriedOrder else order
    }

    @UiThread
    override fun startPurchaseRequest(
        activity: Activity,
        skuDetails: SkuDetails,
        billingClient: BillingClient
    ): BillingResult {
        LogUtil.log.v(TAG, "startPurchaseRequest")
        // Retrieve a value for "skuDetails" by calling querySkuDetailsAsync().
        val flowParams = BillingFlowParams.newBuilder()
            .setSkuDetails(skuDetails)
            .build()

        // UI flow will start
        val result = billingClient.launchBillingFlow(activity, flowParams)
        Log.d(TAG, "Purchased flow finished : $result")

        return result
    }

    override fun processUpdatedPurchases(billingResult: BillingResult?, purchases: MutableList<Purchase>?) {
        LogUtil.log.v(TAG, "processUpdatedPurchases")
        BillingResponse.logResult(billingResult)
        if (purchases.isNullOrEmpty()) {
            val order = Order(
                billingResult = billingResult,
                msg = "Null/Empty list of purchases"
            )
            if (billingResult == null) {
                isQueriedOrders = true
                // only queried orders start with a null billingResult
                this.queriedOrder.postValue(order)
            } else {
                isQueriedOrders = false
                this.order.postValue(order)
            }
            Log.d(TAG, "No purchases available")
        } else {
            for (p in purchases) {
                when (p.purchaseState) {
                    Purchase.PurchaseState.PURCHASED -> processValidation(p)
                    Purchase.PurchaseState.PENDING -> processPendingTransaction(p)
                    Purchase.PurchaseState.UNSPECIFIED_STATE -> processPurchasingError(billingResult)
                }
            }
        }
    }

    override fun processValidation(purchase: Purchase) {
        LogUtil.log.v(TAG, "processValidation")

        if (isNewPurchase(purchase)) {
            orderValidatorListener?.validate(purchase, validation) ?: LogUtil.log.e(TAG, "Null validator object. Cannot complete order.")
        } else {
            LogUtil.log.e(TAG, "Purchase failed verification. Cannot complete order.")
        }
    }

    private fun processPurchase(purchase: Purchase) {
        val type = inventory.allProducts[purchase.sku]?.type

        if (type?.equals(BillingClient.SkuType.INAPP, ignoreCase = true) == true) {
            processInAppPurchase(purchase)
        }

        if (type?.equals(BillingClient.SkuType.SUBS, ignoreCase = true) == true) {
            processSubscription(purchase)
        }
    }

    override fun isNewPurchase(purchase: Purchase): Boolean {
        LogUtil.log.v(TAG, "isPurchaseValid")
        if (purchase.isAcknowledged) {
            // Note: If you do not acknowledge a purchase within three days,
            // the user automatically receives a refund, and Google Play revokes the purchase.
            LogUtil.log.w(TAG, "Purchase item: $purchase, is already Acknowledged. Cannot complete order.")
            return false
        }
        // todo - can provide more validation checks here for checking for new purchases
        return true
    }

    override fun processInAppPurchase(purchase: Purchase) {
        LogUtil.log.v(TAG, "processInAppPurchase")
        if (!inventory.isConsumable(purchase)) {
            processConsumable(purchase)
        } else {
            processNonConsumable(purchase)
        }
    }

    override fun processPendingTransaction(purchase: Purchase) {
        LogUtil.log.v(TAG, "processInAppPurchase")
        if (pendingPurchases.containsValue(purchase)) {
            LogUtil.log.v(TAG, "Pending transaction already in process.")
        } else {
            pendingPurchases[purchase.orderId] = purchase
        }
    }

    override fun processPurchasingError(billingResult: BillingResult?) {
        LogUtil.log.e(TAG, "processPurchasingError: $billingResult")
        BillingResponse.logResult(billingResult)
        val order = Order(
            billingResult = billingResult,
            msg = "Error"
        )
        this.order.postValue(order)
    }

    private fun processConsumable(purchase: Purchase) {
        LogUtil.log.v(TAG, "processConsumable")
        orderUpdateListener?.resumeOrder(purchase, Product.Type.CONSUMABLE)
    }

    private fun processNonConsumable(purchase: Purchase) {
        LogUtil.log.v(TAG, "processNonConsumable")
        orderUpdateListener?.resumeOrder(purchase, Product.Type.NON_CONSUMABLE)
    }

    override fun processSubscription(purchase: Purchase) {
        LogUtil.log.v(TAG, "processSubscription")
        orderUpdateListener?.resumeOrder(purchase, Product.Type.SUBSCRIPTION)
    }

    override fun destroy() {
        isQueriedOrders = false
    }

    companion object {
        private const val TAG = "CustomerSales"
    }
}
