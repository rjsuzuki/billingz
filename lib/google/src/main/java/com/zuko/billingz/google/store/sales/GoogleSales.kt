/*
 *
 *  * Copyright 2021 rjsuzuki
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  *
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
import com.android.billingclient.api.PurchasesResponseListener
import com.android.billingclient.api.SkuDetails
import com.android.billingclient.api.queryPurchaseHistory
import com.zuko.billingz.core.LogUtilz
import com.zuko.billingz.core.misc.BillingzDispatcher
import com.zuko.billingz.core.misc.Dispatcherz
import com.zuko.billingz.core.store.client.Clientz
import com.zuko.billingz.core.store.model.OrderHistoryz
import com.zuko.billingz.core.store.model.Orderz
import com.zuko.billingz.core.store.model.Productz
import com.zuko.billingz.core.store.model.QueryResult
import com.zuko.billingz.core.store.sales.OrderOptions
import com.zuko.billingz.core.store.sales.Salez
import com.zuko.billingz.google.store.client.GoogleClient
import com.zuko.billingz.google.store.inventory.GoogleInventory
import com.zuko.billingz.google.store.model.GoogleOrder
import com.zuko.billingz.google.store.model.GoogleOrderHistory
import com.zuko.billingz.google.store.model.GoogleOrderHistoryQuery
import com.zuko.billingz.google.store.model.GoogleOrdersQuery
import com.zuko.billingz.google.store.model.GoogleProduct
import com.zuko.billingz.google.store.model.GoogleReceipt
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val client: GoogleClient,
    private var dispatcher: Dispatcherz = BillingzDispatcher()
) : Salez {

    private val mainScope = MainScope()

    private var obfuscatedAccountId: String? = null
    private var obfuscatedProfileId: String? = null

    override val currentOrder: MutableLiveData<Orderz> = MutableLiveData<Orderz>()
    override val currentReceipt: MutableLiveData<GoogleReceipt> = MutableLiveData<GoogleReceipt>() // todo - unused - consider deprecating

    /**
     * Purchase history
     */
    override var orderHistoryLiveData: MutableLiveData<GoogleOrderHistory> = MutableLiveData()
    override var orderHistoryStateFlow: MutableStateFlow<GoogleOrderHistory?> = MutableStateFlow(null)
    override var orderHistoryState = orderHistoryStateFlow.asStateFlow()

    /**
     * Validation checks
     */
    override var orderUpdaterListener: Salez.OrderUpdaterListener? = null
    override var orderValidatorListener: Salez.OrderValidatorListener? = null
    private var isAlreadyQueried = false // prevents redundant queries
    @Deprecated("TBD")
    private var isQueriedOrders = false // prevents redundant queries

    /**
     * Orders that requires attention, e.g pending orders, waiting for user interaction,
     * incomplete purchase flows from a bad network, etc.
     */
    private var queriedOrderStateFlow: MutableStateFlow<GoogleOrder?> = MutableStateFlow(null)
    private var queriedOrderState = queriedOrderStateFlow.asStateFlow()
    private var queriedOrderLiveData: MutableLiveData<GoogleOrder?> = MutableLiveData()
    private var pendingOrders = ArrayMap<String, GoogleOrder>()

    /**
     * Entitlements
     */
    private var activeSubscriptions = ArrayMap<String, GoogleReceipt>()
    private var activeInAppProducts = ArrayMap<String, GoogleReceipt>()

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
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                val order = GoogleOrder(
                    purchase = null,
                    billingResult = BillingResult.newBuilder()
                        .setResponseCode(BillingClient.BillingResponseCode.ERROR)
                        .setDebugMessage("Can't start purchase flow with null parameters")
                        .build()
                )
                failedOrder(order)
            }
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
                cancelOrder(order)
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
        order.state = Orderz.State.VALIDATING
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
                                    order.purchase
                                )
                            }
                            Productz.Type.NON_CONSUMABLE -> {
                                completeNonConsumable(
                                    client.getBillingClient(),
                                    order.purchase
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
        LogUtilz.log.v(TAG, "externally canceled order: $order")
        order.state = Orderz.State.CANCELED
        orderUpdaterListener?.onFailure(order)
        currentOrder.postValue(order)
    }

    override fun failedOrder(order: Orderz) {
        LogUtilz.log.e(TAG, "internally failed order: $order")
        order.state = Orderz.State.FAILED
        orderUpdaterListener?.onFailure(order)
        currentOrder.postValue(order)
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
     * For continuing an order in progress from BillingClient.PurchasesUpdatedListener,
     * and for resolving queried orders from BillingClient.queryPurchasesAsync()
     */
    internal fun processUpdatedPurchases(
        billingResult: BillingResult?,
        purchases: MutableList<Purchase>?
    ) {
        LogUtilz.log.v(TAG, "processUpdatedPurchases: purchase list size:${purchases?.size ?: 0}")
        GoogleResponse.logResult(billingResult)

        if (purchases.isNullOrEmpty()) {
            isQueriedOrders = false
            LogUtilz.log.d(TAG, "No purchases available to resolve from queryPurchasesAsync")
        } else {
            mainScope.launch(dispatcher.io()) {
                for (p in purchases) {
                    if (billingResult == null) {
                        // only queried orders start with a null billingResult
                        isQueriedOrders = true
                        val order = GoogleOrder(
                            purchase = p,
                            billingResult = null
                        )
                        order.state = Orderz.State.PROCESSING
                        queriedOrderLiveData.postValue(order)
                        queriedOrderStateFlow.emit(order)
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
    }

    private fun processValidation(purchase: Purchase, billingResult: BillingResult?) {
        LogUtilz.log.v(TAG, "processValidation: $purchase")
        GoogleResponse.logResult(billingResult)

        if (isNewPurchase(purchase)) {
            val order = GoogleOrder(
                purchase = purchase,
                billingResult = billingResult
            )
            validateOrder(order)
        } else {
            LogUtilz.log.e(TAG, "Purchase failed verification. Cannot complete order.")
            val order = GoogleOrder(
                purchase = purchase,
                billingResult = BillingResult.newBuilder()
                    .setResponseCode(BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED)
                    .setDebugMessage("Product has already been acknowledged with Google Play.")
                    .build()
            )
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
     *  Transactions that require one or more additional steps between when a user initiates
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
            mainScope.launch {
                val order = GoogleOrder(
                    purchase = purchase,
                    billingResult = billingResult
                )
                pendingOrders[purchase.orderId] = order
                queriedOrderLiveData.postValue(order)
                queriedOrderStateFlow.emit(order)
            }
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
            billingResult = billingResult
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
                LogUtilz.log.d(TAG, msg)
                val order = GoogleOrder(
                    purchase = purchase,
                    billingResult = billingResult
                )
                order.state = Orderz.State.COMPLETE
                val receipt = GoogleReceipt(
                    purchase = purchase,
                    userId = obfuscatedAccountId,
                    order = order
                )
                currentReceipt.postValue(receipt)
                orderUpdaterListener?.onComplete(receipt)
                currentOrder.postValue(order)
            } else {
                msg = billingResult.debugMessage
                val order = GoogleOrder(
                    purchase = purchase,
                    billingResult = billingResult
                )
                failedOrder(order)
                LogUtilz.log.e(TAG, "Error purchasing consumable. $msg")
            }
        }
    }

    private fun completeNonConsumable(
        billingClient: BillingClient?,
        purchase: Purchase?
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
                    billingResult = billingResult
                )
                LogUtilz.log.d(TAG, "Non-Consumable successfully acknowledged")
                order.state = Orderz.State.COMPLETE
                val receipt = GoogleReceipt(
                    purchase = purchase,
                    userId = obfuscatedAccountId,
                    order = order
                )
                currentReceipt.postValue(receipt)
                orderUpdaterListener?.onComplete(receipt)
                currentOrder.postValue(order)
            } else {
                val order = GoogleOrder(
                    purchase = purchase,
                    billingResult = billingResult
                )
                failedOrder(order)
            }
        }

        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken)
                mainScope.launch(dispatcher.io()) {
                    billingClient?.acknowledgePurchase(acknowledgePurchaseParams.build(), listener)
                }
            } else {
                val order = GoogleOrder(
                    purchase = purchase,
                    billingResult = BillingResult.newBuilder()
                        .setResponseCode(BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED)
                        .setDebugMessage("Non-Consumable has already been acknowledged with Google Play.")
                        .build()
                )
                failedOrder(order)
            }
        } else {
            val order = GoogleOrder(
                purchase = purchase,
                billingResult = BillingResult.newBuilder()
                    .setResponseCode(BillingClient.BillingResponseCode.ITEM_NOT_OWNED)
                    .setDebugMessage("Non-Consumable has not been successfully purchased.")
                    .build()
            )
            failedOrder(order)
        }
    }

    private fun completeSubscription(
        billingClient: BillingClient?,
        purchase: Purchase?
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
                    billingResult = billingResult
                )
                LogUtilz.log.d(TAG, "Subscription successfully acknowledged")
                order.state = Orderz.State.COMPLETE
                val receipt = GoogleReceipt(
                    purchase = purchase,
                    userId = obfuscatedAccountId,
                    order = order
                )
                currentReceipt.postValue(receipt)
                orderUpdaterListener?.onComplete(receipt)
                currentOrder.postValue(order)
            } else {
                val order = GoogleOrder(
                    purchase = purchase,
                    billingResult = billingResult
                )
                failedOrder(order)
            }
        }

        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) { // redundancy checks
            if (!purchase.isAcknowledged) { // redundancy checks
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                billingClient
                    ?.acknowledgePurchase(acknowledgePurchaseParams.build(), listener)
            } else {
                val order = GoogleOrder(
                    purchase = purchase,
                    billingResult = BillingResult.newBuilder()
                        .setResponseCode(BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED)
                        .setDebugMessage("Subscription has already been acknowledged with Google Play.")
                        .build()
                )
                failedOrder(order)
            }
        } else {
            val order = GoogleOrder(
                purchase = purchase,
                billingResult = BillingResult.newBuilder()
                    .setResponseCode(BillingClient.BillingResponseCode.ITEM_NOT_OWNED)
                    .setDebugMessage("Subscription has not been successfully purchased.")
                    .build()
            )
            failedOrder(order)
        }
    }

    override fun refreshQueries() {
        LogUtilz.log.v(TAG, "refreshQueries")
        isAlreadyQueried = if (isAlreadyQueried) {
            LogUtilz.log.d(TAG, "Skipping purchase history refresh.")
            // skip - prevents double queries on initialization
            false
        } else {
            LogUtilz.log.d(TAG, "Refreshing purchase history.")
            queryReceipts()
            queryOrders()
            true
        }
    }

    override fun queryOrders(): QueryResult<Orderz> {
        LogUtilz.log.v(TAG, "queryOrders")
        querySubscriptions()
        queryInAppProducts()
        return GoogleOrdersQuery(this)
    }

    internal fun queryOrdersLiveData(): LiveData<GoogleOrder?> {
        LogUtilz.log.v(TAG, "queryOrdersLiveData")
        return queriedOrderLiveData
    }

    internal fun queryOrdersStateFlow(): StateFlow<GoogleOrder?> {
        LogUtilz.log.v(TAG, "queryOrdersStateFlow")
        return queriedOrderState
    }

    override fun queryReceipts(type: Productz.Type?): QueryResult<OrderHistoryz> {
        LogUtilz.log.v(TAG, "queryReceipts: $type")
        if (client.isReady()) {
            LogUtilz.log.i(TAG, "Fetching all $type purchases made by user.")
            mainScope.launch(dispatcher.io()) {
                queryOrderHistory(type)
            }
        } else {
            LogUtilz.log.e(TAG, "Android BillingClient was not ready yet to continue queryPurchases()")
        }
        return GoogleOrderHistoryQuery(this)
    }

    internal fun queryReceiptsLiveData(): MutableLiveData<GoogleOrderHistory> {
        LogUtilz.log.v(TAG, "queryOrders")
        return orderHistoryLiveData
    }

    internal fun queryReceiptsStateFlow(): StateFlow<GoogleOrderHistory?> {
        LogUtilz.log.v(TAG, "queryOrders")
        return orderHistoryState
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

        mainScope.launch(dispatcher.io()) {
            client.getBillingClient()?.queryPurchaseHistory(skuType)?.let { purchaseHistoryResult ->

                val billingResult = purchaseHistoryResult.billingResult
                val records = purchaseHistoryResult.purchaseHistoryRecordList
                // log billingResult
                GoogleResponse.logResult(billingResult)

                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                }

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

                    val receipts = ArrayMap<String, GoogleReceipt>()
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

                    val orderHistory = GoogleOrderHistory(receipts = receipts)
                    orderHistoryLiveData.postValue(orderHistory)
                    orderHistoryStateFlow.emit(orderHistory)
                }
            }
        }
    }

    override fun destroy() {
        LogUtilz.log.v(TAG, "destroy")
        isQueriedOrders = false
        mainScope.cancel()
    }

    companion object {
        private const val TAG = "BillingzGoogleSales"
    }
}
