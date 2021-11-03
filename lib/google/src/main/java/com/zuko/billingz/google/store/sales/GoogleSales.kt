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
import android.os.Bundle
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
import com.android.billingclient.api.queryPurchaseHistory
import com.zuko.billingz.core.LogUtilz
import com.zuko.billingz.core.store.client.Clientz
import com.zuko.billingz.core.store.model.Orderz
import com.zuko.billingz.core.store.model.Productz
import com.zuko.billingz.core.store.model.Receiptz
import com.zuko.billingz.core.store.sales.OrderOptions
import com.zuko.billingz.core.store.sales.Salez
import com.zuko.billingz.google.store.client.GoogleClient
import com.zuko.billingz.google.store.inventory.GoogleInventory
import com.zuko.billingz.google.store.model.GoogleOrder
import com.zuko.billingz.google.store.model.GoogleProduct
import com.zuko.billingz.google.store.model.GoogleReceipt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.util.Date

/**
 * Representation of the sales department of a store.
 * Lifecycle of an Order = {Start -> Process -> Complete}
 * @constructor
 * @param inventory
 */
class GoogleSales(
    private val inventory: GoogleInventory,
    private val client: GoogleClient
) : Salez {

    private val mainScope = MainScope()

    private var obfuscatedAccountId: String? = null
    private var obfuscatedProfileId: String? = null

    override var currentReceipt = MutableLiveData<Receiptz>() // todo - unused
    override var orderHistory: MutableLiveData<ArrayMap<String, Receiptz>> = MutableLiveData()
    override var orderUpdaterListener: Salez.OrderUpdaterListener? = null
    override var orderValidatorListener: Salez.OrderValidatorListener? = null

    private var isAlreadyQueried = false // prevents redundant queries
    private var isQueriedOrders = false // prevents redundant queries

    /**
     * Orders that requires attention, e.g pending orders, waiting for user interaction,
     * incomplete purchase flows from a bad network, etc.
     */
    private var queriedOrder = MutableLiveData<Orderz>()
    private var pendingOrders = ArrayMap<String, Orderz>()

    private var activeSubscriptions = ArrayMap<String, Receiptz>()
    private var activeInAppProducts = ArrayMap<String, Receiptz>()

    override fun setObfuscatedIdentifiers(accountId: String?, profileId: String?) {
        LogUtilz.log.d(
            TAG,
            "Setting obfuscated identifiers (" +
                    "\n Account ID: $accountId," +
                    "\n Profile ID: $profileId" +
                    "\n )"
        )
        obfuscatedAccountId = accountId
        obfuscatedProfileId = profileId
    }

    // step 1
    override fun startOrder(
        activity: Activity?,
        product: Productz,
        client: Clientz,
        options: Bundle?
    ) {
        LogUtilz.log.v(TAG, "Starting Order for sku: ${product.sku}")
        if (product is GoogleProduct && client is GoogleClient) {
            val result = if (product.type == Productz.Type.SUBSCRIPTION) {
                startSubscriptionPurchaseFlow(
                    activity = activity,
                    newSku = product.skuDetails,
                    billingClient = client.getBillingClient()
                )
            } else {
                startInAppPurchaseFlow(
                    activity = activity,
                    skuDetails = product.skuDetails,
                    billingClient = client.getBillingClient()
                )
            }
            GoogleResponse.logResult(result)
        }
    }

    // step 2
    override fun validateOrder(order: Orderz) {
        order.state = Orderz.State.VALIDATING
        val validatorCallback: Salez.ValidatorCallback = object : Salez.ValidatorCallback {
            override fun validated(order: Orderz) {
                processOrder(order)
            }

            override fun invalidated(order: Orderz) {
                failedOrder(order)
            }
        }
        orderValidatorListener?.validate(order, validatorCallback) ?: LogUtilz.log.e(TAG, "Null validator object. Cannot complete order.")
    }

    // step 3
    override fun processOrder(order: Orderz) {
        LogUtilz.log.v(TAG, "processOrder")
        /**
         * Another validation check point was originally here, but deemed unnecessary.
         * Now, this function immediately proceeds to completeOrder() to consume/acknowledge
         * purchase.
         */
        order.state = Orderz.State.PROCESSING
        completeOrder(order)
    }

    // step 4
    override fun completeOrder(order: Orderz) {
        LogUtilz.log.v(TAG, "completeOrder")

        if (order is GoogleOrder) {
            if (order.skus?.isNullOrEmpty() == false) {
                // get product
                for (sku in order.skus!!) {
                    inventory.getProduct(sku)?.let { product ->
                        when (product.type) {
                            Productz.Type.SUBSCRIPTION -> {
                                completeSubscription(
                                    client.getBillingClient(),
                                    order.purchase,
                                    mainScope = mainScope
                                )
                            }
                            Productz.Type.NON_CONSUMABLE -> {
                                completeNonConsumable(
                                    client.getBillingClient(),
                                    order.purchase,
                                    mainScope = mainScope
                                )
                            }
                            Productz.Type.CONSUMABLE -> {
                                completeConsumable(
                                    client.getBillingClient(),
                                    order.purchase
                                )
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
        // todo - maybe remove
        orderUpdaterListener?.onError(order)
    }

    override fun failedOrder(order: Orderz) {
        LogUtilz.log.e(TAG, "failed order: $order")
        if (order.state == Orderz.State.FAILED) {
            orderUpdaterListener?.onError(order)
        }
    }

    /**
     * When a user upgrades, downgrades, or resignup from your app before the subscription expires,
     * the old subscription is invalidated, and a new subscription is created with a new purchase token.
     */
    @UiThread
    private fun startSubscriptionPurchaseFlow(
        activity: Activity?,
        newSku: SkuDetails?,
        billingClient: BillingClient?,
        options: Bundle? = null
    ): BillingResult {
        LogUtilz.log.v(TAG, "Starting subscription purchase flow")
        if (activity == null || newSku == null || billingClient == null)
            return BillingResult.newBuilder()
                .setResponseCode(BillingClient.BillingResponseCode.ERROR)
                .setDebugMessage("Can't start subscription purchase flow with null parameters")
                .build()

        // Retrieve a value for "skuDetails" by calling querySkuDetailsAsync().
        // Google uses [setObfuscatedAccountId, setObfuscatedProfileId] to detect suspicious behavior
        // and block some types of fraudulent transactions before they are completed.
        // Google Play recommends that you use either encryption or a one-way hash to
        // generate an obfuscated identifier
        val flowParams = BillingFlowParams.newBuilder()
            .setSkuDetails(newSku)

        obfuscatedAccountId?.let {
            flowParams.setObfuscatedAccountId(it)
        }
        obfuscatedProfileId?.let {
            flowParams.setObfuscatedProfileId(it)
        }

        if (options?.getBoolean(OrderOptions.IS_SUB_CHANGE_KEY) == true) {
            val prorationMode = options.getInt(
                OrderOptions.PRORATION_MODE_KEY,
                BillingFlowParams.ProrationMode.IMMEDIATE_WITH_TIME_PRORATION
            )
            val oldSubSku = options.getString(OrderOptions.OLD_SUB_SKU_KEY)

            if (oldSubSku.isNullOrBlank())
                return BillingResult.newBuilder()
                    .setResponseCode(BillingClient.BillingResponseCode.ERROR)
                    .setDebugMessage("Subscription modification requires the product id of the currently active subscription")
                    .build()

            if (activeSubscriptions.containsKey(oldSubSku)) {
                LogUtilz.log.d(
                    TAG,
                    "Subscription to replace confirmed:" +
                            "\n old sku: $oldSubSku" +
                            "\n proration mode: $prorationMode"
                )
                // start upgrade or downgrade
                activeSubscriptions[oldSubSku]?.entitlement?.let { oldPurchaseToken ->
                    val subUpdateParams = BillingFlowParams.SubscriptionUpdateParams.newBuilder()
                        .setOldSkuPurchaseToken(oldPurchaseToken)
                        .setReplaceSkusProrationMode(prorationMode)
                        .build()
                    flowParams.setSubscriptionUpdateParams(subUpdateParams)
                }
            }
        }

        // UI flow will start
        val result = billingClient.launchBillingFlow(activity, flowParams.build())
        LogUtilz.log.v(TAG, "Purchase flow UI finished with response code: ${result.responseCode}")
        GoogleResponse.logResult(result)
        return result
    }

    @UiThread
    private fun startInAppPurchaseFlow(
        activity: Activity?,
        skuDetails: SkuDetails?,
        billingClient: BillingClient?
    ): BillingResult {
        LogUtilz.log.v(TAG, "Starting in-app purchase flow")
        if (activity == null || skuDetails == null || billingClient == null)
            return BillingResult.newBuilder()
                .setResponseCode(BillingClient.BillingResponseCode.ERROR)
                .setDebugMessage("Can't start in-app purchase flow with null parameters")
                .build()

        val flowParams = BillingFlowParams.newBuilder()
            .setSkuDetails(skuDetails)

        obfuscatedAccountId?.let {
            flowParams.setObfuscatedAccountId(it)
        }
        obfuscatedProfileId?.let {
            flowParams.setObfuscatedProfileId(it)
        }

        // UI flow will start
        val result = billingClient.launchBillingFlow(activity, flowParams.build())
        LogUtilz.log.v(TAG, "Purchased flow finished : $result")
        return result
    }

    /**
     * For resolving queried purchases from BillingClient.queryPurchasesAsync().
     *
     */
    fun processUpdatedPurchases(
        billingResult: BillingResult?,
        purchases: MutableList<Purchase>?
    ) {
        LogUtilz.log.v(TAG, "processUpdatedPurchases: purchase list size:${purchases?.size ?: 0}")
        GoogleResponse.logResult(billingResult)

        if (purchases.isNullOrEmpty()) {
            isQueriedOrders = false
            LogUtilz.log.d(TAG, "No purchases available to resolve from queryPurchasesAsync")
        } else {
            for (p in purchases) {
                if (billingResult == null) {
                    // only queried orders start with a null billingResult
                    isQueriedOrders = true
                    val order = GoogleOrder(
                        purchase = p,
                        billingResult = null,
                        msg = "Queried Order"
                    )
                    this.queriedOrder.postValue(order)
                } else {
                    isQueriedOrders = false
                    when (p.purchaseState) {
                        Purchase.PurchaseState.PURCHASED -> processValidation(p, billingResult)
                        Purchase.PurchaseState.PENDING -> processPendingTransaction(p, billingResult)
                        Purchase.PurchaseState.UNSPECIFIED_STATE -> processPurchasingError(p, billingResult)
                    }
                }
            }
        }
    }

    private fun processValidation(purchase: Purchase, billingResult: BillingResult?) {
        LogUtilz.log.v(TAG, "processValidation: $purchase")
        GoogleResponse.logResult(billingResult)

        val order = GoogleOrder(
            purchase = purchase,
            billingResult = billingResult,
            msg = "new purchase in progress"
        )
        order.state = Orderz.State.PROCESSING

        if (isNewPurchase(purchase)) {
            validateOrder(order)
        } else {
            LogUtilz.log.e(TAG, "Purchase failed verification. Cannot complete order.")
            failedOrder(order)
        }
    }

    private fun isNewPurchase(purchase: Purchase): Boolean {
        LogUtilz.log.v(TAG, "isPurchaseValid: $purchase")
        if (purchase.isAcknowledged) {
            // Note: If you do not acknowledge a purchase within three days,
            // the user automatically receives a refund, and Google Play revokes the purchase.
            LogUtilz.log.w(TAG, "Purchase item: $purchase, is already Acknowledged. Cannot complete order.")
            return false
        }
        return true
    }

    /**
     *  transactions that require one or more additional steps between when a user initiates
     *  a purchase and when the payment method for the purchase is processed
     */
    private fun processPendingTransaction(purchase: Purchase, billingResult: BillingResult?) {
        LogUtilz.log.d(
            TAG,
            "processPendingTransaction (" +
                    "\n purchase: $purchase," +
                    "\n billingResult: $billingResult" +
                    "\n )"
        )
        GoogleResponse.logResult(billingResult)
        if (pendingOrders.containsKey(purchase.orderId)) {
            LogUtilz.log.v(
                TAG,
                "Pending transaction already in processing for orderId: ${purchase.orderId}"
            )
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
        LogUtilz.log.e(
            TAG,
            "processPurchasingError (" +
                    "\n purchase: $purchase," +
                    "\n billingResult: $billingResult" +
                    "\n )"
        )
        GoogleResponse.logResult(billingResult)
        val order = GoogleOrder(
            purchase = purchase,
            billingResult = billingResult,
            msg = "Error"
        )
        failedOrder(order)
    }

    private fun completeConsumable(billingClient: BillingClient?, purchase: Purchase?) {
        LogUtilz.log.v(TAG, "completeConsumable: $purchase")

        if (billingClient?.isReady == false) {
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
                LogUtilz.log.d(TAG, "Product successfully purchased and consumed: ${purchase.orderId}")
                val order = GoogleOrder(
                    purchase = purchase,
                    billingResult = billingResult,
                    msg = msg
                )
                order.state = Orderz.State.COMPLETE
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
                failedOrder(order)
                LogUtilz.log.e(TAG, "Error purchasing consumable. $msg")
            }
        }
    }

    private fun completeNonConsumable(
        billingClient: BillingClient?,
        purchase: Purchase?,
        mainScope: CoroutineScope?
    ) {
        LogUtilz.log.v(TAG, "completeNonConsumable: $purchase")
        if (billingClient?.isReady == false) {
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
                order.state = Orderz.State.COMPLETE
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
                failedOrder(order)
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
        LogUtilz.log.v(TAG, "completeSubscription: $purchase")
        if (billingClient?.isReady == false) {
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
                order.state = Orderz.State.COMPLETE
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
                failedOrder(order)
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
            queryOrders()
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
                queryOrderHistory(type)
            }
        } else {
            LogUtilz.log.e(TAG, "Android BillingClient was not ready yet to continue queryPurchases()")
        }
    }

    suspend fun queryReceiptsAsync(type: Productz.Type?): List<Receiptz> = coroutineScope {
        LogUtilz.log.v(TAG, "queryReceiptsAsync: $type")
        if (client.isReady()) {
            LogUtilz.log.i(TAG, "Fetching all $type purchases made by user.")
            val skuType =
                if (type == Productz.Type.SUBSCRIPTION) BillingClient.SkuType.SUBS else BillingClient.SkuType.INAPP

            val purchaseHistoryResult = client.getBillingClient()?.queryPurchaseHistory(skuType)
            if (purchaseHistoryResult?.purchaseHistoryRecordList.isNullOrEmpty()) {
                emptyList()
            } else {
                // convert records into receipts
                val receiptsList = mutableListOf<Receiptz>()
                purchaseHistoryResult?.purchaseHistoryRecordList?.forEach { record ->
                    val receipt = GoogleReceipt(
                        purchase = null
                    )
                    receipt.entitlement = record.purchaseToken
                    receipt.orderDate = Date(record.purchaseTime)
                    receipt.skus = record.skus
                    receipt.originalJson = record.originalJson
                    receipt.quantity = record.quantity
                    receipt.signature = record.signature
                    receiptsList.add(receipt)
                }
                receiptsList
            }
        } else {
            LogUtilz.log.e(TAG, "Android BillingClient was not ready yet to continue queryPurchases()")
            emptyList()
        }
    }

    private fun querySubscriptions() {
        LogUtilz.log.v(TAG, "querySubscriptions")

        val purchaseResponseListener =
            PurchasesResponseListener { billingResult, purchases ->
                GoogleResponse.logResult(billingResult)
                LogUtilz.log.d(TAG, "Queried Purchases (SUB): $purchases")

                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    if (purchases.isNotEmpty()) {
                        activeSubscriptions.clear()
                        purchases.forEach { p ->
                            val receipt = GoogleReceipt(purchase = p)
                            activeSubscriptions[receipt.entitlement] = receipt
                        }
                    }
                    processUpdatedPurchases(billingResult, purchases)
                    LogUtilz.log.i(TAG, "Subscription order history received: $purchases")
                }
            }

        client.getBillingClient()?.queryPurchasesAsync(BillingClient.SkuType.SUBS, purchaseResponseListener)
    }

    private fun queryInAppProducts() {
        LogUtilz.log.v(TAG, "queryInAppProducts")

        val purchaseResponseListener =
            PurchasesResponseListener { billingResult, purchases ->
                GoogleResponse.logResult(billingResult)
                LogUtilz.log.d(TAG, "Queried Purchases (IAP): $purchases")

                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    if (purchases.isNotEmpty()) {
                        activeInAppProducts.clear()
                        purchases.forEach { p ->
                            val receipt = GoogleReceipt(purchase = p)
                            activeInAppProducts[receipt.entitlement] = receipt
                        }
                    }
                    processUpdatedPurchases(billingResult, purchases)
                    LogUtilz.log.i(TAG, "In-app order history received: $purchases")
                }
            }

        client.getBillingClient()?.queryPurchasesAsync(BillingClient.SkuType.INAPP, purchaseResponseListener)
    }

    private fun queryOrderHistory(type: Productz.Type?) {
        LogUtilz.log.v(TAG, "queryOrderHistory: $type")
        val skuType =
            if (type == Productz.Type.SUBSCRIPTION) BillingClient.SkuType.SUBS else BillingClient.SkuType.INAPP
        val purchaseHistoryResponseListener =
            PurchaseHistoryResponseListener { billingResult, records ->
                // handle billingResult
                GoogleResponse.logResult(billingResult)

                if (records.isNullOrEmpty()) {
                    LogUtilz.log.w(TAG, "No receipts found for product type: $type")
                    // notify empty list
                } else {
                    // convert records into receipts
                    if (skuType == BillingClient.SkuType.SUBS) {
                        activeSubscriptions.clear()
                    } else {
                        activeInAppProducts.clear()
                    }

                    val receipts = ArrayMap<String, Receiptz>()
                    records.forEach { record ->
                        val receipt = GoogleReceipt(
                            purchase = null
                        )
                        receipt.entitlement = record.purchaseToken
                        receipt.orderDate = Date(record.purchaseTime)
                        receipt.skus = record.skus
                        receipt.originalJson = record.originalJson
                        receipt.quantity = record.quantity
                        receipt.signature = record.signature
                        receipts[receipt.entitlement] = receipt
                    }
                    orderHistory.postValue(receipts)
                }
            }
        client.getBillingClient()?.queryPurchaseHistoryAsync(skuType, purchaseHistoryResponseListener)
    }

    override fun destroy() {
        LogUtilz.log.v(TAG, "destroy")
        isQueriedOrders = false // todo - verify behavior
    }

    companion object {
        private const val TAG = "BillingzGoogleSales"
    }
}
