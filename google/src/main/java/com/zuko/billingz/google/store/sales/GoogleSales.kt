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
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.AcknowledgePurchaseResponseListener
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchaseHistoryResponseListener
import com.android.billingclient.api.PurchasesResponseListener
import com.android.billingclient.api.SkuDetails
import com.zuko.billingz.google.store.client.GoogleClient
import com.zuko.billingz.google.store.inventory.GoogleInventory
import com.zuko.billingz.google.store.model.GoogleOrder
import com.zuko.billingz.google.store.model.GoogleProduct
import com.zuko.billingz.google.store.model.GoogleReceipt
import com.zuko.billingz.core.LogUtilz
import com.zuko.billingz.core.store.Securityz
import com.zuko.billingz.google.store.model.BillingResponsez
import com.zuko.billingz.core.store.client.Clientz
import com.zuko.billingz.core.store.model.Orderz
import com.zuko.billingz.core.store.model.Productz
import com.zuko.billingz.core.store.model.Receiptz
import com.zuko.billingz.core.store.sales.Salez
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Representation of the sales department of a store.
 * Lifecycle of an Order = {Start -> Process -> Complete}
 * @constructor
 * @param inventory
 */
class GoogleSales(private val inventory: GoogleInventory,
                  private val client: GoogleClient) : Salez {

    private val mainScope = MainScope()

    private var obfuscatedAccountId: String? = null
    private var obfuscatedProfileId: String? = null

    override var currentReceipt = MutableLiveData<Receiptz>() //todo - unused
    override var orderHistory: MutableLiveData<List<Receiptz>> = MutableLiveData()
    override var orderUpdaterListener: Salez.OrderUpdaterListener? = null
    override var orderValidatorListener: Salez.OrderValidatorListener? = null

    private var isAlreadyQueried = false // prevents redundant queries
    private var isQueriedOrders = false  // prevents redundant queries

    /**
     * Orders that requires attention, e.g pending orders, waiting for user interaction,
     * incomplete purchase flows from a bad network, etc.
     */
    private var queriedOrder = MutableLiveData<Orderz>()

    /**
     * Cache for pending orders
     */
    private var pendingOrders = ArrayMap<String, Orderz>()

    // subscriptions that require attention
    private var activeSubscriptions: MutableList<Purchase> = mutableListOf()

    // in-app products that require attention
    private var activeInAppProducts: MutableList<Purchase> = mutableListOf()

    override fun setObfuscatedIdentifiers(accountId: String?, profileId: String?) {
        LogUtilz.log.d(TAG, "Setting obfuscated identifiers (" +
                "\n Account ID: $accountId," +
                "\n Profile ID: $profileId" +
                "\n )")
        obfuscatedAccountId = accountId
        obfuscatedProfileId = profileId
    }

    // step 1
    override fun startOrder(activity: Activity?, product: Productz, client: Clientz) {
        if(product is GoogleProduct && client is GoogleClient) {
            startPurchaseRequest(activity, product.skuDetails, client.getBillingClient())
        }
    }

    // step 2
    override fun validateOrder(order: Orderz) {
        val validatorCallback: Salez.ValidatorCallback = object : Salez.ValidatorCallback {
            override fun validated(order: Orderz) {
                processOrder(order)
            }

            override fun invalidated(order: Orderz) {
                cancelOrder(order)
            }
        }
        orderValidatorListener?.validate(order, validatorCallback) ?: LogUtilz.log.e(TAG, "Null validator object. Cannot complete order.")
    }

    // step 3
    override fun processOrder(order: Orderz) {
        LogUtilz.log.v(TAG, "processOrder")
        val updaterCallback: Salez.UpdaterCallback = object : Salez.UpdaterCallback {
            override fun complete(order: Orderz) {
                completeOrder(order)
            }

            override fun cancel(order: Orderz) {
                cancelOrder(order)
            }
        }

        if(order is GoogleOrder) {
            order.purchase?.let { p ->
                orderUpdaterListener?.onResume(order, updaterCallback)
            }
        }
    }

    // step 4
    override fun completeOrder(order: Orderz) {
        LogUtilz.log.v(TAG, "completeOrder")

        if(order is GoogleOrder) {
            if(order.skus?.isNullOrEmpty() == false) {
                // get product
                for(sku in order.skus!!) {
                    inventory.getProduct(sku)?.let { product ->
                        when(product.type) {
                            Productz.Type.SUBSCRIPTION -> {
                                completeSubscription(
                                    client.getBillingClient(),
                                    order.purchase,
                                    mainScope = mainScope)
                            }
                            Productz.Type.NON_CONSUMABLE -> {
                                completeNonConsumable(
                                    client.getBillingClient(),
                                    order.purchase,
                                    mainScope = mainScope)
                            }
                            Productz.Type.CONSUMABLE -> {
                                //todo - this probably needs to wait for user action
                                completeConsumable(
                                    client.getBillingClient(),
                                    order.purchase)
                            }
                            else -> {
                                LogUtilz.log.e(TAG, "error completing order")
                            }
                        }
                    }
                }
            }
        }
    }

    override fun cancelOrder(order: Orderz) {
        LogUtilz.log.v(TAG, "cancel order: $order")
        TODO("Not yet implemented")
    }

    override fun failedOrder(order: Orderz) {
        LogUtilz.log.e(TAG, "failed order: $order")
        if(order.state == Orderz.State.FAILED) {

            // check if purchaseToken is unique (if qu
        }
    }

    @UiThread
    private fun startPurchaseRequest(
        activity: Activity?,
        skuDetails: SkuDetails?,
        billingClient: BillingClient?
    ): BillingResult {
        LogUtilz.log.v(TAG, "startPurchaseRequest")
        if(activity == null || skuDetails == null || billingClient == null)
            return BillingResult.newBuilder()
                .setResponseCode(BillingClient.BillingResponseCode.ERROR)
                .setDebugMessage("Can't start purchase flow with null parameters")
                .build()

        // Retrieve a value for "skuDetails" by calling querySkuDetailsAsync().
        // Google uses [setObfuscatedAccountId, setObfuscatedProfileId] to detect suspicious behavior
        // and block some types of fraudulent transactions before they are completed.
        // Google Play recommends that you use either encryption or a one-way hash to
        // generate an obfuscated identifier
        val flowParams = BillingFlowParams.newBuilder()
            .setSkuDetails(skuDetails)

        obfuscatedAccountId?.let {
            flowParams.setObfuscatedAccountId(it)
        }
        obfuscatedProfileId?.let {
            flowParams.setObfuscatedProfileId(it)
        }

        if(skuDetails.type == BillingClient.SkuType.SUBS) {
            val subParams = getSubscriptionUpdateParams(
                oldPurchaseToken = "",
                prorationMode = BillingFlowParams.ProrationMode.IMMEDIATE_WITH_TIME_PRORATION
            )
            subParams?.let {
                flowParams.setSubscriptionUpdateParams(it)
            }
        }
        flowParams.build()

        // UI flow will start
        val result = billingClient.launchBillingFlow(activity, flowParams.build())
        Log.d(TAG, "Purchased flow finished : $result")

        return result
    }

    private fun getSubscriptionUpdateParams(oldPurchaseToken: String, prorationMode: Int): BillingFlowParams.SubscriptionUpdateParams? {
        return BillingFlowParams.SubscriptionUpdateParams.newBuilder()
            .setOldSkuPurchaseToken(oldPurchaseToken)
            .setReplaceSkusProrationMode(prorationMode)
            .build()
    }

    fun processUpdatedPurchases(billingResult: BillingResult?, purchases: MutableList<Purchase>?) {
        LogUtilz.log.v(TAG, "processUpdatedPurchases: ${purchases?.size ?: 0 }")
        BillingResponsez.logResult(billingResult)

        if (purchases.isNullOrEmpty()) {
            val order = GoogleOrder(
                billingResult = billingResult,
                msg = "Null/Empty list of purchases"
            )
            order.state = Orderz.State.PROCESSING
            if (billingResult == null) {
                isQueriedOrders = true
                // only queried orders start with a null billingResult
                this.queriedOrder.postValue(order)
            } else {
                isQueriedOrders = false
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
        LogUtilz.log.v(TAG, "processValidation")
        BillingResponsez.logResult(billingResult)
        if (isNewPurchase(purchase)) {
            val order = GoogleOrder(
                purchase = purchase,
                billingResult = billingResult,
                msg = "new purchase"
            )
            order.state = Orderz.State.VALIDATING
            validateOrder(order)
        } else {
            LogUtilz.log.e(TAG, "Purchase failed verification. Cannot complete order.")
        }
    }

    private fun isNewPurchase(purchase: Purchase): Boolean {
        LogUtilz.log.v(TAG, "isPurchaseValid")
        if (purchase.isAcknowledged) {
            // Note: If you do not acknowledge a purchase within three days,
            // the user automatically receives a refund, and Google Play revokes the purchase.
            LogUtilz.log.w(TAG, "Purchase item: $purchase, is already Acknowledged. Cannot complete order.")
            return false
        }
        // todo - can provide more validation checks here for checking for new purchases
        return true
    }

    /**
     *  transactions that require one or more additional steps between when a user initiates
     *  a purchase and when the payment method for the purchase is processed
     */
    private fun processPendingTransaction(purchase: Purchase, billingResult: BillingResult?) {
        LogUtilz.log.d(TAG, "processPendingTransaction (" +
                "\n purchase: $purchase," +
                "\n billingResult: $billingResult" +
                "\n )")
        BillingResponsez.logResult(billingResult)
        if (pendingOrders.containsKey(purchase.orderId)) {
            LogUtilz.log.v(TAG, "Pending transaction already in processing for orderId: ${purchase.orderId}")
        } else {
            val order = GoogleOrder(
                purchase = purchase,
                billingResult = billingResult,
                msg = "pending process..."
            )
            pendingOrders[purchase.orderId] = order
            queriedOrder.postValue(order)
        }
    }

    private fun processPurchasingError(purchase: Purchase, billingResult: BillingResult?) {
        LogUtilz.log.e(TAG, "processPurchasingError (" +
                "\n purchase: $purchase," +
                "\n billingResult: $billingResult" +
                "\n )")
        BillingResponsez.logResult(billingResult)
        val order = GoogleOrder(
            purchase = purchase,
            billingResult = billingResult,
            msg = "Error"
        )
        orderUpdaterListener?.onError(order)
    }

    private fun completeConsumable(
        billingClient: BillingClient?,
        purchase: Purchase?) {

        if(billingClient?.isReady == false) {
            LogUtilz.log.wtf(TAG, "billing client is not ready")
            return
        }
        purchase ?: return

        val consumeParams = ConsumeParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient?.consumeAsync(consumeParams) { billingResult, p ->
            val msg: String
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                msg = "Consumable successfully purchased: $p"
                Log.d(TAG, "Product successfully purchased and consumed: ${purchase.orderId}")
                val order = GoogleOrder(
                    purchase = purchase,
                    billingResult = billingResult,
                    msg = msg
                )
                val receipt = GoogleReceipt(
                    purchase = purchase,
                    userId = obfuscatedAccountId,
                    order = order
                )
                currentReceipt.postValue(receipt)
                orderUpdaterListener?.onComplete(receipt)
            } else {
                msg = billingResult.debugMessage
                val order = GoogleOrder(
                    purchase = purchase,
                    billingResult = billingResult,
                    msg = msg
                )
                orderUpdaterListener?.onError(order)
                LogUtilz.log.e(TAG, "Error purchasing consumable. $msg")
            }
        }
    }

    private fun completeNonConsumable(
        billingClient: BillingClient?,
        purchase: Purchase?,
        mainScope: CoroutineScope?
    ) {
        if(billingClient?.isReady == false) {
            LogUtilz.log.wtf(TAG, "billing client is not ready")
            return
        }
        purchase ?: return

        val listener = AcknowledgePurchaseResponseListener { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val order = GoogleOrder(
                    purchase = purchase,
                    billingResult = billingResult,
                    msg = "Non-Consumable successfully acknowledged"
                )
                val receipt = GoogleReceipt(
                    purchase = purchase,
                    userId = obfuscatedAccountId,
                    order = order
                )
                currentReceipt.postValue(receipt)
                orderUpdaterListener?.onComplete(receipt)
            } else {
                val order = GoogleOrder(
                    purchase = purchase,
                    billingResult = billingResult,
                    msg = "Non-Consumable acknowledgment error"
                )
                orderUpdaterListener?.onError(order)
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
        mainScope: CoroutineScope?
    ) {
        if(billingClient?.isReady == false) {
            LogUtilz.log.wtf(TAG, "billing client is not ready")
            return
        }
        purchase ?: return

        val listener = AcknowledgePurchaseResponseListener { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val order = GoogleOrder(
                    purchase = purchase,
                    billingResult = billingResult,
                    msg = "Subscription successfully acknowledged"
                )
                val receipt = GoogleReceipt(
                    purchase = purchase,
                    userId = obfuscatedAccountId,
                    order = order
                )
                currentReceipt.postValue(receipt)
                orderUpdaterListener?.onComplete(receipt)
            } else {
                val order = GoogleOrder(
                    purchase = purchase,
                    billingResult = billingResult,
                    msg = "Subscription acknowledgment error"
                )
                orderUpdaterListener?.onError(order)
            }
        }

        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                billingClient
                    ?.acknowledgePurchase(acknowledgePurchaseParams.build(), listener)
            }
        }
    }

    override fun refreshQueries() {
        LogUtilz.log.v(TAG, "refreshQueries")
        if (isAlreadyQueried) {
            LogUtilz.log.d(TAG, "Skipping purchase history refresh.")
            // skip - prevents double queries on initialization
            isAlreadyQueried = false
        } else {
            LogUtilz.log.d(TAG, "Refreshing purchase history.")
            queryReceipts()
            isAlreadyQueried = true
        }
    }

    override fun queryOrders(): LiveData<Orderz> {
        LogUtilz.log.v(TAG, "queryOrders")
        querySubscriptions()
        queryInAppProducts()
        return queriedOrder
    }

    override fun queryReceipts(type: Productz.Type?) {
        LogUtilz.log.v(TAG, "queryReceipts: $type")
        if (client.isReady()) {
            LogUtilz.log.i(TAG, "Fetching all $type purchases made by user.")
            mainScope.launch(Dispatchers.IO) {
                queryPurchaseHistory(type)
            }
        } else {
            LogUtilz.log.e(TAG, "Android BillingClient was not ready yet to continue queryPurchases()")
        }
    }

    private fun querySubscriptions() {
        LogUtilz.log.v(TAG, "querySubscriptions")

        val purchaseResponseListener =
            PurchasesResponseListener { billingResult, purchases ->
                BillingResponsez.logResult(billingResult)
                LogUtilz.log.d(TAG, "Purchases: $purchases")

                if(billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    activeSubscriptions.addAll(purchases)
                    processUpdatedPurchases(billingResult, activeSubscriptions)
                    LogUtilz.log.i(TAG, "Subscription order history received: $purchases")
                }
            }

        client.getBillingClient()?.queryPurchasesAsync(BillingClient.SkuType.SUBS, purchaseResponseListener)
    }

    private fun queryInAppProducts() {
        LogUtilz.log.v(TAG, "queryInAppProducts")

        val purchaseResponseListener =
            PurchasesResponseListener { billingResult, purchases ->
                BillingResponsez.logResult(billingResult)
                LogUtilz.log.d(TAG, "Purchases: $purchases")

                if(billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    activeInAppProducts.addAll(purchases)
                    processUpdatedPurchases(billingResult, activeInAppProducts)
                    LogUtilz.log.i(TAG, "In-app order history received: $purchases")
                }
            }

        client.getBillingClient()?.queryPurchasesAsync(BillingClient.SkuType.INAPP, purchaseResponseListener)
    }

    private fun queryPurchaseHistory(type: Productz.Type?) {
        LogUtilz.log.v(TAG, "queryPurchaseHistory: $type")
        val skuType = if(type == Productz.Type.SUBSCRIPTION) BillingClient.SkuType.SUBS else BillingClient.SkuType.INAPP
        val purchaseHistoryResponseListener =
            PurchaseHistoryResponseListener { billingResult, records ->
                // handle billingResult
                BillingResponsez.logResult(billingResult)

                // todo - purchase history records
                if(records.isNullOrEmpty()) {
                    // notify empty list
                } else {
                    // convert records into receipts
                }
            }
        client.getBillingClient()?.queryPurchaseHistoryAsync(skuType, purchaseHistoryResponseListener)
    }

    override fun destroy() {
        LogUtilz.log.v(TAG, "destroy")
        mainScope.cancel()
        isQueriedOrders = false // todo - verify behavior
    }

    companion object {
        private const val TAG = "CustomerSales"
    }
}
