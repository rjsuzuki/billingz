package com.zuko.billingz.lib.sales

import android.app.Activity
import androidx.collection.ArrayMap
import androidx.lifecycle.MutableLiveData
import com.android.billingclient.api.*
import com.zuko.billingz.lib.extra.CleanUp
import com.zuko.billingz.lib.products.Product

/**
 *
 */
interface Sales: CleanUp {

    /**
     *
     */
    var orderUpdateListener: OrderUpdateListener?

    /**
     *
     */
    var orderValidatorListener: OrderValidatorListener?

    /**
     *
     */
    var purchasesUpdatedListener: PurchasesUpdatedListener

    /**
     * Provides a liveData [Order] object for
     * developers to observe and react to on
     * the UI/Main thread.
     * Objects can be passed from the normal purchase flow
     * or when the app is verifying a list of queried purchases.
     */
    var order: MutableLiveData<Order>

    /**
     *
     */
    var queriedOrder: MutableLiveData<Order>

    /**
     *
     */
    fun getOrderOrQueried(): MutableLiveData<Order>

    /**
     *
     * ArrayMap<OrderId, Purchase>
     */
    var pendingPurchases: ArrayMap<String, Purchase>

    /**
     *
     */
    fun startPurchaseRequest(activity: Activity,
                             skuDetails: SkuDetails,
                             billingClient: BillingClient) : BillingResult

    /**
     * Handler method for responding to updates from Android's PurchaseUpdatedListener class
     * or when checking results from queryPurchases()
     * @param billingResult
     * @param purchases
     * Should run on background thread.
     */
    fun processUpdatedPurchases(billingResult: BillingResult?, purchases: MutableList<Purchase>?)

    /**
     *
     */
    fun processValidation(purchase: Purchase)

    /**
     * Simple validation checks before
     * allowing developer to implement their
     * validator
     */
    fun isNewPurchase(purchase: Purchase) : Boolean

    /**
     *
     */
    fun processInAppPurchase(purchase: Purchase)

    /**
     *
     */
    fun processSubscription(purchase: Purchase)

    /**
     *
     */
    fun processPendingTransaction(purchase: Purchase)

    /**
     *
     */
    fun processPurchasingError(billingResult: BillingResult?)

    /**
     * Purchases can be made outside of app, or finish while app is in background.
     * show in-app popup, or deliver msg to an inbox, or use an OS notification
     */
    // fun notifyPurchase()

    /**
     * Set by Manager class
     */
    interface OrderUpdateListener {
        fun resumeOrder(purchase: Purchase, productType: Product.ProductType)
    }

    /**
     * For dev to implement
     */
    // Verify the purchase.
    // Ensure entitlement was not already granted for this purchaseToken.
    // Grant entitlement to the user.
    interface OrderValidatorListener {
        fun validate(purchase: Purchase, callback: ValidatorCallback)
    }

    /**
     * Respond to the events triggered by the developer's validator
     */
    interface ValidatorCallback {
        fun onSuccess(purchase: Purchase)
        fun onFailure(purchase: Purchase)
    }
}