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
import com.android.billingclient.api.BillingFlowParams.SubscriptionUpdateParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchaseHistoryResult
import com.android.billingclient.api.PurchasesResponseListener
import com.android.billingclient.api.QueryPurchaseHistoryParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.SkuDetails
import com.android.billingclient.api.queryPurchaseHistory
import com.zuko.billingz.core.misc.BillingzDispatcher
import com.zuko.billingz.core.misc.Dispatcherz
import com.zuko.billingz.core.misc.Logger
import com.zuko.billingz.core.store.client.Clientz
import com.zuko.billingz.core.store.model.OrderHistoryz
import com.zuko.billingz.core.store.model.Orderz
import com.zuko.billingz.core.store.model.Productz
import com.zuko.billingz.core.store.model.QueryResult
import com.zuko.billingz.core.store.sales.Optionz
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
@Suppress("DEPRECATION")
class GoogleSales(
    private val inventory: GoogleInventory,
    private val client: GoogleClient,
    private var dispatcher: Dispatcherz = BillingzDispatcher()
) : Salez {

    override var isNewVersion = false
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
        Logger.d(
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
        Logger.v(TAG, "Starting Order for sku: ${product.getProductId()}")
        Logger.d(
            TAG,
            "startOrder => " +
                "\n product: $product," +
                "\n options: $options"
        )

        val newOrder = GoogleOrder(null, null)
        product.getProductId()?.let { productId ->
            newOrder.skus = listOf(productId)
        }
        currentOrder.postValue(newOrder)
        if (product is GoogleProduct && client is GoogleClient) {
            val result = if (product.type == Productz.Type.SUBSCRIPTION) {
                startSubscriptionPurchaseFlow(
                    activity = activity,
                    skuDetails = product.getSkuDetails(),
                    productDetails = product.getProductDetails(),
                    billingClient = client.getBillingClient(),
                    options = options
                )
            } else {
                startInAppPurchaseFlow(
                    activity = activity,
                    skuDetails = product.getSkuDetails(),
                    productDetails = product.getProductDetails(),
                    billingClient = client.getBillingClient(),
                    options = options
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

    // step 2a
    override fun validateOrder(order: Orderz) {
        Logger.v(TAG, "validateOrder: ${order.orderId}")
        order.state = Orderz.State.VALIDATING
        val validatorCallback: Salez.ValidatorCallback = object : Salez.ValidatorCallback {
            override fun validated(order: Orderz) {
                completeOrder(order)
            }

            override fun invalidated(order: Orderz) {
                failedOrder(order)
            }
        }
        orderValidatorListener?.validate(order, validatorCallback) ?: Logger.e(TAG, "Null validator object. Cannot complete order.")
    }

    // step 2b
    override fun processOrder(order: Orderz) {
        Logger.v(TAG, "processOrder: ${order.orderId}")
        order.state = Orderz.State.PROCESSING
        if (order is GoogleOrder) {
            mainScope.launch {
                queriedOrderLiveData.postValue(order)
                queriedOrderStateFlow.emit(order)
                validateOrder(order)
            }
        }
    }

    // step 3
    override fun completeOrder(order: Orderz) {
        Logger.v(TAG, "completeOrder: ${order.orderId}")

        if (order is GoogleOrder) {
            if (!order.skus.isNullOrEmpty()) {
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
                                Logger.e(TAG, "error completing order")
                            }
                        }
                    }
                }
            }
        }
    }

    override fun cancelOrder(order: Orderz) {
        Logger.v(TAG, "externally canceled order: ${order.orderId}")
        order.state = Orderz.State.CANCELED
        order.skus = currentOrder.value?.skus
        orderUpdaterListener?.onCanceled(order)
        currentOrder.postValue(order)
    }

    override fun failedOrder(order: Orderz) {
        Logger.e(TAG, "internally failed order: ${order.orderId}")
        order.state = Orderz.State.FAILED
        order.skus = currentOrder.value?.skus
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
        skuDetails: SkuDetails?,
        productDetails: ProductDetails?,
        billingClient: BillingClient?,
        options: Bundle? = null
    ): BillingResult {
        Logger.v(TAG, "Starting subscription purchase flow")
        Logger.d(
            TAG,
            "startSubscriptionPurchaseFlow =>" +
                "\n skuDetails: $skuDetails," +
                "\n productDetails: $productDetails," +
                "\n options: $options," +
                "\n isNewVersion: $isNewVersion"
        )
        if (activity == null || (!isNewVersion && skuDetails == null) || (isNewVersion && productDetails == null) || billingClient == null) {
            return BillingResult.newBuilder()
                .setResponseCode(BillingClient.BillingResponseCode.ERROR)
                .setDebugMessage("Can't start subscription purchase flow with null parameters")
                .build()
        }

        // Retrieve a value for "skuDetails" by calling querySkuDetailsAsync().
        // Google uses [setObfuscatedAccountId, setObfuscatedProfileId] to detect suspicious behavior
        // and block some types of fraudulent transactions before they are completed.
        // Google Play recommends that you use either encryption or a one-way hash to
        // generate an obfuscated identifier.
        // Billingz uses a one-way hash.

        val flowParams = BillingFlowParams.newBuilder()

        if (isNewVersion) {
            productDetails?.subscriptionOfferDetails?.let { subscriptionOfferDetails ->
                val selectedOfferId = options?.getString(Optionz.Type.SELECTED_OFFER_ID.name, null)
                val selectedOfferIndex = options?.getInt(Optionz.Type.SELECTED_OFFER_INDEX.name, -1) ?: -1

                val offerToken = when {
                    !selectedOfferId.isNullOrBlank() && subscriptionOfferDetails.find({ it.offerId == selectedOfferId }) != null -> {
                        subscriptionOfferDetails.find { it.offerId == selectedOfferId }?.offerToken
                    }
                    selectedOfferIndex > -1 && selectedOfferIndex < subscriptionOfferDetails.size -> {
                        subscriptionOfferDetails[selectedOfferIndex]?.offerToken
                    }
                    else -> {
                        subscriptionOfferDetails.firstOrNull()?.offerToken
                    }
                }

                if (offerToken != null) {
                    val productDetailsParamsList =
                        listOf(
                            BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(productDetails)
                                .setOfferToken(offerToken)
                                .build()
                        )
                    flowParams.setProductDetailsParamsList(productDetailsParamsList)
                } else {
                    Logger.w(TAG, "Subscription OfferToken is null.")
                }
            } ?: Logger.w(TAG, "ProductDetails.subscriptionOfferDetails cannot be null")
        } else {
            skuDetails?.let {
                flowParams.setSkuDetails(skuDetails)
            } ?: Logger.w(TAG, "skuDetails cannot be null")
        }

        obfuscatedAccountId?.let {
            flowParams.setObfuscatedAccountId(it)
        }
        obfuscatedProfileId?.let {
            flowParams.setObfuscatedProfileId(it)
        }

        // We can indefinitely add support for more options since we are utilizing an Android Bundle objects
        options?.let {
            // for EU personalized pricing disclosure requirements
            val isOfferPersonalized = options.getBoolean(Optionz.Type.IS_PERSONALIZED_OFFER.name, false)
            flowParams.setIsOfferPersonalized(isOfferPersonalized)

            // Note: oldSubId is mainly for simple validation and logging
            val oldSubId = options.getString(Optionz.Type.OLD_SUB_ID.name, null)
            val oldPurchaseToken = options.getString(Optionz.Type.OLD_PURCHASE_TOKEN.name, null)
            val prorationMode = options.getInt(
                Optionz.Type.PRORATION_MODE.name,
                SubscriptionUpdateParams.ReplacementMode.DEFERRED
            )

            when {
                oldPurchaseToken.isNullOrBlank() && !oldSubId.isNullOrBlank() -> {
                    return BillingResult.newBuilder()
                        .setResponseCode(BillingClient.BillingResponseCode.ERROR)
                        .setDebugMessage("Subscription modification requires the purchase token of the currently active subscription")
                        .build()
                }
                !oldPurchaseToken.isNullOrBlank() && oldSubId.isNullOrBlank() -> {
                    return BillingResult.newBuilder()
                        .setResponseCode(BillingClient.BillingResponseCode.ERROR)
                        .setDebugMessage("Subscription modification requires the product id of the currently active subscription")
                        .build()
                }
                !oldPurchaseToken.isNullOrBlank() && !oldSubId.isNullOrBlank() -> {
                    Logger.d(
                        TAG,
                        "Subscription to replace confirmed:" +
                            "\n old product id: $oldSubId," +
                            "\n old purchase token: $oldPurchaseToken," +
                            "\n new proration mode: $prorationMode"
                    )
                    val subUpdateParams = SubscriptionUpdateParams.newBuilder()
                        .setSubscriptionReplacementMode(prorationMode)
                        .setOldPurchaseToken(oldPurchaseToken)
                        .build()
                    flowParams.setSubscriptionUpdateParams(subUpdateParams)
                }
                else -> {} // ignore
            }
        }

        // UI flow will start
        val result = billingClient.launchBillingFlow(activity, flowParams.build())
        Logger.v(TAG, "Purchase flow UI finished with response code: ${result.responseCode}")
        return result
    }

    @UiThread
    private fun startInAppPurchaseFlow(
        activity: Activity?,
        skuDetails: SkuDetails?,
        productDetails: ProductDetails?,
        billingClient: BillingClient?,
        options: Bundle? = null
    ): BillingResult {
        Logger.v(TAG, "Starting in-app purchase flow")
        Logger.d(
            TAG,
            "startInAppPurchaseFlow =>" +
                "\n skuDetails: $skuDetails," +
                "\n productDetails: $productDetails," +
                "\n options: $options," +
                "\n isNewVersion: $isNewVersion"
        )
        if (activity == null || (!isNewVersion && skuDetails == null) || (isNewVersion && productDetails == null) || billingClient == null) {
            return BillingResult.newBuilder()
                .setResponseCode(BillingClient.BillingResponseCode.ERROR)
                .setDebugMessage("Can't start in-app purchase flow with null parameters")
                .build()
        }

        val flowParams = BillingFlowParams.newBuilder()

        if (isNewVersion) {
            productDetails?.let {
                val productDetailsParamsList =
                    listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(productDetails)
                            .build()
                    )
                flowParams.setProductDetailsParamsList(productDetailsParamsList)
            } ?: Logger.w(TAG, "productDetails cannot be null")
        } else {
            skuDetails?.let {
                flowParams.setSkuDetails(skuDetails)
            } ?: Logger.w(TAG, "skuDetails cannot be null")
        }

        obfuscatedAccountId?.let {
            flowParams.setObfuscatedAccountId(it)
        }
        obfuscatedProfileId?.let {
            flowParams.setObfuscatedProfileId(it)
        }

        options?.let {
            // for EU personalized pricing disclosure requirements
            val isOfferPersonalized =
                options.getBoolean(Optionz.Type.IS_PERSONALIZED_OFFER.name, false)
            flowParams.setIsOfferPersonalized(isOfferPersonalized)
        }

        // UI flow will start
        val result = billingClient.launchBillingFlow(activity, flowParams.build())
        Logger.v(TAG, "Purchased flow finished: $result")
        return result
    }

    /**
     * For continuing an order in progress from BillingClient.PurchasesUpdatedListener,
     * and for resolving queried orders from BillingClient.queryPurchasesAsync()
     */
    internal fun processUpdatedPurchases(
        billingResult: BillingResult?,
        purchases: List<Purchase>?
    ) {
        Logger.v(TAG, "processUpdatedPurchases=> purchase list size: ${purchases?.size ?: 0}")
        GoogleResponse.logResult(billingResult)

        if (purchases.isNullOrEmpty()) {
            // This either occurs when a user opens the billing flow UI and then closes it and chooses to not
            // continue with the purchase, or when this class calls [refreshQueries].
            isQueriedOrders = false
            Logger.d(TAG, "No purchases available to resolve from queryPurchasesAsync")
            if (billingResult?.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
                val order = GoogleOrder(null, billingResult)
                cancelOrder(order)
            }
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
                        processOrder(order)
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
        Logger.v(TAG, "processValidation: $purchase")
        GoogleResponse.logResult(billingResult)

        if (isNewPurchase(purchase)) {
            val order = GoogleOrder(
                purchase = purchase,
                billingResult = billingResult
            )
            validateOrder(order)
        } else {
            Logger.e(TAG, "Purchase failed verification. Cannot complete order.")
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
        Logger.v(TAG, "isPurchaseValid: $purchase")
        if (purchase.isAcknowledged) {
            // Note: If you do not acknowledge a purchase within three days,
            // the user automatically receives a refund, and Google Play revokes the purchase.
            Logger.w(TAG, "Purchase item: $purchase, is already Acknowledged. Cannot complete order.")
            return false
        }
        return true
    }

    /**
     *  Transactions that require one or more additional steps between when a user initiates
     *  a purchase and when the payment method for the purchase is processed
     */
    private fun processPendingTransaction(purchase: Purchase, billingResult: BillingResult?) {
        Logger.d(
            TAG,
            "processPendingTransaction (" +
                "\n purchase: $purchase," +
                "\n billingResult: $billingResult" +
                "\n )"
        )
        GoogleResponse.logResult(billingResult)
        if (pendingOrders.containsKey(purchase.orderId)) {
            Logger.v(
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
        Logger.e(
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
        Logger.v(TAG, "completeConsumable: $purchase")

        if (billingClient?.isReady == false) {
            Logger.wtf(TAG, "billing client is not ready")
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
                Logger.d(TAG, msg)
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
                Logger.e(TAG, "Error purchasing consumable. $msg")
            }
        }
    }

    private fun completeNonConsumable(
        billingClient: BillingClient?,
        purchase: Purchase?
    ) {
        Logger.v(TAG, "completeNonConsumable: $purchase")
        if (billingClient?.isReady == false) {
            Logger.wtf(TAG, "billing client is not ready")
            return
        }
        purchase ?: return

        val listener = AcknowledgePurchaseResponseListener { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val order = GoogleOrder(
                    purchase = purchase,
                    billingResult = billingResult
                )
                Logger.d(TAG, "Non-Consumable successfully acknowledged")
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
        Logger.v(TAG, "completeSubscription: $purchase")
        if (billingClient?.isReady == false) {
            Logger.wtf(TAG, "billing client is not ready")
            return
        }
        purchase ?: return

        val listener = AcknowledgePurchaseResponseListener { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val order = GoogleOrder(
                    purchase = purchase,
                    billingResult = billingResult
                )
                Logger.d(TAG, "Subscription successfully acknowledged")
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
        Logger.v(TAG, "refreshQueries")
        isAlreadyQueried = if (isAlreadyQueried) {
            Logger.d(TAG, "Skipping purchase history refresh.")
            // skip - prevents double queries on initialization
            false
        } else {
            Logger.d(TAG, "Refreshing purchase history.")
            queryReceipts()
            queryOrders()
            true
        }
    }

    override fun queryOrders(): QueryResult<Orderz> {
        Logger.v(TAG, "queryOrders")
        querySubscriptions()
        queryInAppProducts()
        return GoogleOrdersQuery(this)
    }

    internal fun queryOrdersLiveData(): LiveData<GoogleOrder?> {
        Logger.v(TAG, "queryOrdersLiveData")
        return queriedOrderLiveData
    }

    internal fun queryOrdersStateFlow(): StateFlow<GoogleOrder?> {
        Logger.v(TAG, "queryOrdersStateFlow")
        return queriedOrderState
    }

    override fun queryReceipts(type: Productz.Type?): QueryResult<OrderHistoryz> {
        Logger.v(TAG, "queryReceipts: $type")
        if (client.isReady()) {
            Logger.i(TAG, "Fetching all $type purchases made by user.")
            mainScope.launch(dispatcher.io()) {
                queryOrderHistory(type)
            }
        } else {
            Logger.e(TAG, "Android BillingClient was not ready yet to continue queryPurchases()")
        }
        return GoogleOrderHistoryQuery(this)
    }

    internal fun queryReceiptsLiveData(): MutableLiveData<GoogleOrderHistory> {
        Logger.v(TAG, "queryOrders")
        return orderHistoryLiveData
    }

    internal fun queryReceiptsStateFlow(): StateFlow<GoogleOrderHistory?> {
        Logger.v(TAG, "queryOrders")
        return orderHistoryState
    }

    /**
     * Per Google: Returns only active subscriptions and non-consumed one-time purchases.
     */
    private fun querySubscriptions() {
        Logger.v(TAG, "querySubscriptions")

        val purchaseResponseListener =
            PurchasesResponseListener { billingResult, purchases ->
                GoogleResponse.logResult(billingResult)
                Logger.d(TAG, "Queried Purchases (SUB): $purchases")

                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    if (purchases.isNotEmpty()) {
                        activeSubscriptions.clear()
                        purchases.forEach { p ->
                            val receipt = GoogleReceipt(purchase = p)
                            activeSubscriptions[receipt.entitlement] = receipt
                        }
                    }
                    Logger.i(TAG, "Subscription order history received: $purchases")
                }
                processUpdatedPurchases(billingResult, purchases)
            }
        if (isNewVersion) {
            val params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
            client.getBillingClient()?.queryPurchasesAsync(params, purchaseResponseListener)
        } else {
            client.getBillingClient()?.queryPurchasesAsync(BillingClient.ProductType.SUBS, purchaseResponseListener)
        }
    }

    private fun queryInAppProducts() {
        Logger.v(TAG, "queryInAppProducts")

        val purchaseResponseListener =
            PurchasesResponseListener { billingResult, purchases ->
                GoogleResponse.logResult(billingResult)
                Logger.d(TAG, "Queried Purchases (IAP): $purchases")

                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    if (purchases.isNotEmpty()) {
                        activeInAppProducts.clear()
                        purchases.forEach { p ->
                            val receipt = GoogleReceipt(purchase = p)
                            activeInAppProducts[receipt.entitlement] = receipt
                        }
                    }
                    Logger.i(TAG, "In-app order history received: $purchases")
                }
                processUpdatedPurchases(billingResult, purchases)
            }

        if (isNewVersion) {
            val params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
            client.getBillingClient()?.queryPurchasesAsync(params, purchaseResponseListener)
        } else {
            client.getBillingClient()?.queryPurchasesAsync(BillingClient.ProductType.INAPP, purchaseResponseListener)
        }
    }

    /**
     * Per Google: Returns the most recent purchase made by the user for each product,
     * even if that purchase is expired, canceled, or consumed.
     */
    private fun queryOrderHistory(type: Productz.Type?) {
        Logger.v(TAG, "queryOrderHistory: $type")
        val skuType =
            if (type == Productz.Type.SUBSCRIPTION) {
                BillingClient.ProductType.SUBS
            } else {
                BillingClient.ProductType.INAPP
            }

        mainScope.launch(dispatcher.io()) {
            if (isNewVersion) {
                val params = QueryPurchaseHistoryParams.newBuilder()
                    .setProductType(skuType)
                    .build()
                client.getBillingClient()?.queryPurchaseHistory(params)?.let { purchaseHistoryResult ->
                    processPurchaseHistoryResult(skuType, purchaseHistoryResult)
                }
            } else {
                client.getBillingClient()?.queryPurchaseHistory(skuType)?.let { purchaseHistoryResult ->
                    processPurchaseHistoryResult(skuType, purchaseHistoryResult)
                }
            }
        }
    }

    private suspend fun processPurchaseHistoryResult(type: String, purchaseHistoryResult: PurchaseHistoryResult) {
        Logger.v(TAG, "processPurchaseHistoryResult")
        val billingResult = purchaseHistoryResult.billingResult
        val records = purchaseHistoryResult.purchaseHistoryRecordList
        // log billingResult
        GoogleResponse.logResult(billingResult)

        if (records.isNullOrEmpty()) {
            Logger.w(TAG, "No receipts found for product type: $type")
            // notify empty list
        } else {
            // convert records into receipts
            if (type == BillingClient.ProductType.SUBS) {
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
                if (isNewVersion) {
                    receipt.skus = record.products.toList()
                } else {
                    receipt.skus = record.skus.toList()
                }
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

    override fun destroy() {
        Logger.v(TAG, "destroy")
        isQueriedOrders = false
        mainScope.cancel()
    }

    companion object {
        private const val TAG = "BillingzGoogleSales"
    }
}
