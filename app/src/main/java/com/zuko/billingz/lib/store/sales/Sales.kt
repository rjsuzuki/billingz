package com.zuko.billingz.lib.store.sales

import android.app.Activity
import androidx.collection.ArrayMap
import androidx.lifecycle.MutableLiveData
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.SkuDetails
import com.zuko.billingz.lib.misc.CleanUp
import com.zuko.billingz.lib.store.products.Product

/**
 *
 */
interface Sales : CleanUp {

    /**
     * @see [OrderUpdateListener]
     */
    var orderUpdateListener: OrderUpdateListener?

    /**
     * @see [OrderValidatorListener]
     */
    var orderValidatorListener: OrderValidatorListener?

    /**
     * @see [PurchasesUpdatedListener]
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
     * mutable live data object of an [Order]
     */
    var queriedOrder: MutableLiveData<Order>

    /**
     * @return - mutable live data observable for an [Order]
     */
    fun getOrderOrQueried(): MutableLiveData<Order>

    /**
     *
     * ArrayMap<OrderId, Purchase>
     */
    var pendingPurchases: ArrayMap<String, Purchase>

    /**
     * @param activity
     * @param skuDetails
     * @param billingClient
     * @return [BillingResult]
     */
    fun startPurchaseRequest(
        activity: Activity,
        skuDetails: SkuDetails,
        billingClient: BillingClient
    ): BillingResult

    /**
     * Handler method for responding to updates from Android's PurchaseUpdatedListener class
     * or when checking results from queryPurchases()
     * @param billingResult
     * @param purchases
     * Should run on background thread.
     */
    fun processUpdatedPurchases(billingResult: BillingResult?, purchases: MutableList<Purchase>?)

    /**
     * @param purchase
     */
    fun processValidation(purchase: Purchase)

    /**
     * Simple validation checks before
     * allowing developer to implement their
     * validator
     */
    fun isNewPurchase(purchase: Purchase): Boolean

    /**
     * @param purchase
     */
    fun processInAppPurchase(purchase: Purchase)

    /**
     * @param purchase
     */
    fun processSubscription(purchase: Purchase)

    /**
     * @param purchase
     */
    fun processPendingTransaction(purchase: Purchase)

    /**
     * @param billingResult
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
     * For developers to implement.
     * Enables the ability to verify purchases with your own logic,
     * ensure entitlement was not already granted for this purchaseToken,
     * and grant entitlement to the user.
     */
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
