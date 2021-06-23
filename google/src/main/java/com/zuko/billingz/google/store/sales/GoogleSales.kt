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
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.AcknowledgePurchaseResponseListener
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchaseHistoryRecord
import com.android.billingclient.api.PurchaseHistoryResponseListener
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.SkuDetails
import com.zuko.billingz.google.store.client.GoogleClient
import com.zuko.billingz.google.store.inventory.GoogleInventory
import com.zuko.billingz.google.store.model.GoogleOrder
import com.zuko.billingz.google.store.model.GoogleProduct
import com.zuko.billingz.google.store.model.GoogleReceipt
import com.zuko.billingz.lib.LogUtil
import com.zuko.billingz.lib.misc.BillingResponse
import com.zuko.billingz.lib.store.client.Client
import com.zuko.billingz.lib.store.model.Product
import com.zuko.billingz.lib.store.model.Order
import com.zuko.billingz.lib.store.model.Receipt
import com.zuko.billingz.lib.store.sales.Sales
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Representation of the sales department of a store
 * @constructor
 * @param inventory
 */
class GoogleSales(private val inventory: GoogleInventory, private val client: GoogleClient) : Sales {

    private val mainScope = MainScope()

    override var currentOrder = MutableLiveData<Order>() //todo change to receipt?
    override var currentReceipt = MutableLiveData<Receipt>()
    override var orderHistory: MutableLiveData<List<Receipt>> = MutableLiveData()

    override var orderUpdaterListener: Sales.OrderUpdaterListener? = null
    override var orderValidatorListener: Sales.OrderValidatorListener? = null

    private var isAlreadyQueried = false
    private var isQueriedOrders = false
    private var queriedOrder = MutableLiveData<Order>()
    private var pendingPurchases = ArrayMap<String, Purchase>()
    private var activeSubscriptions: MutableList<Purchase> = mutableListOf()
    private var activeInAppProducts: MutableList<Purchase> = mutableListOf()

    private var purchaseHistoryResponseListener =
        PurchaseHistoryResponseListener { billingResult, records -> // handle billingResult
            // todo - purchase history records
            if(records.isNullOrEmpty()) {

            } else {
                // convert records into receipts
            }
        }


    private val validatorCallback: Sales.ValidatorCallback = object : Sales.ValidatorCallback {
        override fun validated(order: Order) {
            processOrder(order)
        }

        override fun invalidate(order: Order) {
            cancelOrder(order)
        }
    }

    private val updaterCallback: Sales.UpdaterCallback = object : Sales.UpdaterCallback {
        override fun complete(order: Order) {
            completeOrder(order)
        }

        override fun cancel(order: Order) {
            cancelOrder(order)
        }
    }

    // TODO
    fun getOrderOrQueried(): MutableLiveData<Order> {
        return if (isQueriedOrders) queriedOrder else currentOrder
    }

    // step 1
    override fun startOrder(activity: Activity?, product: Product, client: Client) {
        if(product is GoogleProduct && client is GoogleClient) {
            startPurchaseRequest(activity, product.skuDetails, client.getBillingClient())
        }
    }

    // step 2
    override fun validateOrder(order: Order) {
        orderValidatorListener?.validate(order, validatorCallback) ?: LogUtil.log.e(TAG, "Null validator object. Cannot complete order.")
    }

    // step 3
    override fun processOrder(order: Order) {
        LogUtil.log.v(TAG, "processOrder")
        if(order is GoogleOrder) {
            order.purchase?.let { p ->
                orderUpdaterListener?.onResume(order, updaterCallback)
            }
        }
    }

    // step 4
    override fun completeOrder(order: Order) {
        LogUtil.log.v(TAG, "completeOrder")

        if(order is GoogleOrder) {
            when(order.product?.type) {
                Product.Type.SUBSCRIPTION -> {
                    completeSubscription(
                        client.getBillingClient(),
                        order.purchase,
                        this.currentOrder,
                        mainScope = mainScope)
                }
                Product.Type.NON_CONSUMABLE -> {
                    completeNonConsumable(
                        client.getBillingClient(),
                        order.purchase,
                        this.currentOrder,
                        mainScope = mainScope)
                }
                Product.Type.CONSUMABLE -> {
                    completeConsumable(
                        client.getBillingClient(),
                        order.purchase,
                        this.currentOrder,
                        mainScope = mainScope)
                }
                else -> {
                    LogUtil.log.e(TAG, "error completing order")
                }
            }
        }
    }

    override fun cancelOrder(order: Order) {
        TODO("Not yet implemented")
    }

    override fun failedOrder(order: Order) {
        TODO("Not yet implemented")
    }

    override fun refreshQueries() {
        refreshReceipts()
        client.getBillingClient()?.queryPurchaseHistoryAsync(BillingClient.SkuType.INAPP, purchaseHistoryResponseListener)
    }

    override fun queryOrders() {
        client.getBillingClient()?.queryPurchases(BillingClient.SkuType.INAPP)
    }

    fun refreshReceipts() {
        LogUtil.log.v(TAG, "queryPurchases")
        if (isAlreadyQueried) {
            LogUtil.log.d(TAG, "Skipping purchase history refresh.")
            // skip - prevents double queries on initialization
            isAlreadyQueried = false
        } else {
            LogUtil.log.d(TAG, "Refreshing purchase history.")
            queryReceipts()
            isAlreadyQueried = true
        }
    }

    override fun queryReceipts(type: Product.Type?) {
        if (client.isReady()) {
            LogUtil.log.i(TAG, "Fetching all purchases made by user.")

            mainScope.launch(Dispatchers.IO) {
                querySubscriptions()
                queryInAppProducts()
            }
        } else {
            LogUtil.log.e(TAG, "Android BillingClient was not ready yet to continue queryPurchases()")
        }
    }

    @UiThread
    private fun startPurchaseRequest(
        activity: Activity?,
        skuDetails: SkuDetails?,
        billingClient: BillingClient?
    ): BillingResult {
        LogUtil.log.v(TAG, "startPurchaseRequest")
        if(activity == null || skuDetails == null || billingClient == null)
            return BillingResult.newBuilder()
                .setResponseCode(BillingClient.BillingResponseCode.ERROR)
                .setDebugMessage("Can't start purchase flow with null parameters")
                .build()

        // Retrieve a value for "skuDetails" by calling querySkuDetailsAsync().
        val flowParams = BillingFlowParams.newBuilder()
            .setSkuDetails(skuDetails)
            .build()

        // UI flow will start
        val result = billingClient.launchBillingFlow(activity, flowParams)
        Log.d(TAG, "Purchased flow finished : $result")

        return result
    }

    fun processUpdatedPurchases(billingResult: BillingResult?, purchases: MutableList<Purchase>?) {
        LogUtil.log.v(TAG, "processUpdatedPurchases")
        BillingResponse.logResult(billingResult)
        if (purchases.isNullOrEmpty()) {
            val order = GoogleOrder(
                billingResult = billingResult,
                msg = "Null/Empty list of purchases"
            )
            if (billingResult == null) {
                isQueriedOrders = true
                // only queried orders start with a null billingResult
                this.queriedOrder.postValue(order)
            } else {
                isQueriedOrders = false
                this.currentOrder.postValue(order)
            }
            Log.d(TAG, "No purchases available")
        } else {
            for (p in purchases) {
                when (p.purchaseState) {
                    Purchase.PurchaseState.PURCHASED -> processValidation(p, billingResult)
                    Purchase.PurchaseState.PENDING -> processPendingTransaction(p, billingResult)
                    Purchase.PurchaseState.UNSPECIFIED_STATE -> processPurchasingError(p, billingResult)
                }
            }
        }
    }

    private fun processValidation(purchase: Purchase, billingResult: BillingResult?) {
        LogUtil.log.v(TAG, "processValidation")

        if (isNewPurchase(purchase)) {
            val order = GoogleOrder(
                purchase = purchase,
                billingResult = billingResult,
                msg = "new purchase"
            )
            validateOrder(order)
        } else {
            LogUtil.log.e(TAG, "Purchase failed verification. Cannot complete order.")
        }
    }

    private fun isNewPurchase(purchase: Purchase): Boolean {
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

    private fun processPendingTransaction(purchase: Purchase, billingResult: BillingResult?) {
        LogUtil.log.v(TAG, "processInAppPurchase")
        if (pendingPurchases.containsValue(purchase)) {
            LogUtil.log.v(TAG, "Pending transaction already in process.")
        } else {
            pendingPurchases[purchase.orderId] = purchase
        }
    }

    private fun processPurchasingError(purchase: Purchase, billingResult: BillingResult?) {
        LogUtil.log.e(TAG, "processPurchasingError: $billingResult")
        BillingResponse.logResult(billingResult)
        val order = GoogleOrder(
            purchase = purchase,
            billingResult = billingResult,
            msg = "Error"
        )
        this.currentOrder.postValue(order)
    }

    private fun completeConsumable(
        billingClient: BillingClient?,
        purchase: Purchase?,
        order: MutableLiveData<Order>,
        mainScope: CoroutineScope?
    ) {

        //todo - handle null object gracefully
        purchase ?: return

        val consumeParams = ConsumeParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient?.consumeAsync(consumeParams) { billingResult, p ->
            val msg: String
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                msg = "Consumable successfully purchased: $p"
                Log.d(TAG, "Product successfully purchased: ${purchase.sku}")
                val receipt = GoogleReceipt(purchase)
                currentReceipt.postValue(receipt)
                orderUpdaterListener?.onComplete(receipt)
            } else {
                msg = billingResult.debugMessage
                LogUtil.log.e(TAG, "Error purchasing consumable. $msg")
            }
            val data = GoogleOrder(
                purchase = purchase,
                billingResult = billingResult,
                msg = msg
            )
            order.postValue(data)
        }
    }

    private fun completeNonConsumable(
        billingClient: BillingClient?,
        purchase: Purchase?,
        order: MutableLiveData<Order>, //todo - remove
        mainScope: CoroutineScope?
    ) {

        //todo - handle null object gracefully
        purchase ?: return

        val listener = AcknowledgePurchaseResponseListener { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val data = GoogleOrder(
                    purchase = purchase,
                    billingResult = billingResult,
                    msg = "Non-Consumable successfully purchased"
                )
                order.postValue(data)
                val receipt = GoogleReceipt(purchase)
                currentReceipt.postValue(receipt)
                orderUpdaterListener?.onComplete(receipt)
            }
        }

        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken)
                mainScope?.launch(Dispatchers.IO) {
                    billingClient?.acknowledgePurchase(acknowledgePurchaseParams.build(), listener)
                }
            }
        }
    }

    private fun completeSubscription(
        billingClient: BillingClient?,
        purchase: Purchase?,
        order: MutableLiveData<Order>,
        mainScope: CoroutineScope?
    ) {
        //todo - handle null object gracefully
        purchase ?: return

        val listener = AcknowledgePurchaseResponseListener { billingResult ->
            val data = GoogleOrder(
                purchase = purchase,
                billingResult = billingResult,
                msg = "Subscription successfully purchased"
            )
            order.postValue(data)
            val receipt = GoogleReceipt(purchase)
            currentReceipt.postValue(receipt)
            orderUpdaterListener?.onComplete(receipt)
        }

        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)

                mainScope?.launch(Dispatchers.IO) {
                    billingClient
                        ?.acknowledgePurchase(acknowledgePurchaseParams.build(), listener)
                }
            }
        }
    }

    // todo - run async
    private fun querySubscriptions() {
        LogUtil.log.v(TAG, "querySubscriptions")
        val subsResult = client.getBillingClient()?.queryPurchases(BillingClient.SkuType.SUBS)
        if (subsResult?.responseCode == BillingClient.BillingResponseCode.OK) { // todo verify
            subsResult.purchasesList?.let { subscriptions ->
                activeSubscriptions = subscriptions
                if (activeSubscriptions.isNotEmpty()) {
                    processUpdatedPurchases(null, activeSubscriptions)
                    // todo - queryPurchaseHistory(BillingClient.SkuType.SUBS)
                }

                LogUtil.log.i(TAG, "Subscription order history received: $subscriptions")
            } ?: LogUtil.log.d(TAG, "No subscription history available.")
        }
    }

    // todo - run async
    private fun queryInAppProducts() {
        LogUtil.log.v(TAG, "queryInAppProducts")
        val inAppResult = client.getBillingClient()?.queryPurchases(BillingClient.SkuType.INAPP)
        if (inAppResult?.responseCode == BillingClient.BillingResponseCode.OK) { // todo verify
            inAppResult.purchasesList?.let { purchases ->
                activeInAppProducts = purchases
                if (activeInAppProducts.isNotEmpty())
                    processUpdatedPurchases(null, activeSubscriptions)
                LogUtil.log.i(TAG, "In-app order history received: $purchases")
            } ?: LogUtil.log.d(TAG, "No In-app products history available.")
        }
    }

    // when to query history?
    private fun queryPurchaseHistory(skuType: String) {
        client.getBillingClient()?.queryPurchaseHistoryAsync(skuType, purchaseHistoryResponseListener)
    }

    override fun destroy() {
        LogUtil.log.v(TAG, "destroy")
        mainScope.cancel()
        isQueriedOrders = false
    }

    companion object {
        private const val TAG = "CustomerSales"
    }
}
