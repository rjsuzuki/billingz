package com.zuko.billingz.lib.sales

import android.app.Activity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.android.billingclient.api.*
import com.zuko.billingz.lib.client.Billing
import com.zuko.billingz.lib.products.Product
import kotlinx.coroutines.CoroutineScope

interface Sales {

    var orderValidatorListener: OrderValidatorListener?
    var orderUpdatedListener: OrderUpdateListener?
    var purchasesUpdatedListener: PurchasesUpdatedListener

    /**
     * Provides a liveData [Order] object for
     * developers to observe and react to on
     * the UI/Main thread.
     */
    var currentOrder: MutableLiveData<Order>

    fun startPurchaseRequest(activity: Activity,
                             skuDetails: SkuDetails,
                             billingClient: BillingClient) : BillingResult

    /**
     * Handler method for responding to updates from Android's PurchaseUpdatedListener class
     * @param billingResult
     * @param purchases
     * Should run on background thread.
     */
    fun processUpdatedPurchases(billingResult: BillingResult, purchases: MutableList<Purchase>?)

    fun processValidation(purchase: Purchase)

    fun isPurchaseVerified(purchase: Purchase) : Boolean

    fun processInAppPurchase(purchase: Purchase)

    fun processPendingTransaction(purchase: Purchase)

    fun processPurchasingError(billingResult: BillingResult?)

    /**
     * Purchases can be made outside of app, or finish while app is in background.
     * show in-app popup, or deliver msg to an inbox, or use an OS notification
     */
    // fun notifyPurchase()
    interface OrderUpdateListener {
        fun completeOrder(purchase: Purchase, productType: Product.ProductType)
    }
    interface OrderValidatorListener {
        fun isValidPurchase(purchase: Purchase) : Boolean
    }
}