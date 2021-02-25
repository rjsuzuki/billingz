package com.zuko.billingz.lib.sales

import android.app.Activity
import android.util.Log
import androidx.annotation.UiThread
import androidx.collection.ArrayMap
import androidx.lifecycle.MutableLiveData
import com.android.billingclient.api.*
import com.zuko.billingz.lib.LogUtil
import com.zuko.billingz.lib.inventory.Inventory
import com.zuko.billingz.lib.products.Product


class ProductSales(val inventory: Inventory): Sales {


    override var currentOrder = MutableLiveData<Order>()
    override var orderValidatorListener: Sales.OrderValidatorListener? = null
    override var orderUpdatedListener: Sales.OrderUpdateListener? = null

    override var purchasesUpdatedListener: PurchasesUpdatedListener =
        PurchasesUpdatedListener { billingResult, purchases -> processUpdatedPurchases(billingResult, purchases) }

    /**
     *
     * ArrayMap<OrderId, Purchase>
     */
    private var pendingPurchases = ArrayMap<String, Purchase>()

    @UiThread
    override fun startPurchaseRequest(activity: Activity,
                                      skuDetails: SkuDetails,
                                      billingClient: BillingClient): BillingResult {

        // Retrieve a value for "skuDetails" by calling querySkuDetailsAsync().
        val flowParams = BillingFlowParams.newBuilder()
            .setSkuDetails(skuDetails)
            .build()

        // UI flow will start
        val result = billingClient.launchBillingFlow(activity, flowParams)
        Log.d(TAG, "Purchased flow finished : $result")

        return result
    }

    override fun processUpdatedPurchases(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        if(purchases.isNullOrEmpty()) {
            val order = Order(
                billingResult = billingResult,
                msg = "Null/Empty list of purchases"
            )
            currentOrder.postValue(order)
            Log.d(TAG, "No purchases available")
        } else {
            for(p in purchases) {
                when(p.purchaseState) {
                    Purchase.PurchaseState.PURCHASED -> processValidation(p)
                    Purchase.PurchaseState.PENDING -> processPendingTransaction(p)
                    Purchase.PurchaseState.UNSPECIFIED_STATE -> processPurchasingError(null)
                }
            }
        }
    }

    override fun processValidation(purchase: Purchase) {
        if(isPurchaseVerified(purchase)) { //possibly move this into respective methods below

            //is inApp or Sub?
            val type = inventory.allProducts[purchase.sku]?.type

            if(type?.equals(BillingClient.SkuType.INAPP, ignoreCase = true) == true) {
                processInAppPurchase(purchase)
            }

            if(type?.equals(BillingClient.SkuType.SUBS, ignoreCase = true) == true) {
                processSubscription(purchase)
            }
        } else {
            //todo add error handling?
        }
    }

    override fun isPurchaseVerified(purchase: Purchase): Boolean {
        LogUtil.log.d(TAG, "isPurchaseVerified")

        if(purchase.isAcknowledged) {
            //Note: If you do not acknowledge a purchase within three days,
            // the user automatically receives a refund, and Google Play revokes the purchase.
            LogUtil.log.w(TAG, "Purchase item: $purchase, is already Acknowledged. Cannot complete order.")
            return false
        }

        if(orderValidatorListener?.isValidPurchase(purchase) == false) {
            LogUtil.log.w(TAG, "Purchase item: $purchase, is invalid. Cannot complete order.")
            return false
        }
        // Verify the purchase.
        // Ensure entitlement was not already granted for this purchaseToken.
        // Grant entitlement to the user.

        //todo wait for dev server to confirm consumption or wait for Google Play?
        return true
    }

    override fun processInAppPurchase(purchase: Purchase) {
        if(!inventory.isConsumable(purchase)) {
            processConsumable(purchase)
        } else {
            processNonConsumable(purchase)
        }
    }

    override fun processPendingTransaction(purchase: Purchase) {
        if(pendingPurchases.containsValue(purchase)) {
            LogUtil.log.v(TAG, "Pending transaction already in process.")
        } else {
            pendingPurchases[purchase.orderId] = purchase
        }
    }

    override fun processPurchasingError(billingResult: BillingResult?) {
        LogUtil.log.e(TAG, "processPurchasingError: $billingResult")
        val order = Order(
            billingResult = billingResult,
            msg = "Error"
        )
        currentOrder.postValue(order)
    }

    private fun processConsumable(purchase: Purchase) {
        orderUpdatedListener?.completeOrder(purchase, Product.ProductType.CONSUMABLE)
    }

    private fun processNonConsumable(purchase: Purchase) {
        orderUpdatedListener?.completeOrder(purchase, Product.ProductType.NON_CONSUMABLE)
    }

    private fun processSubscription(purchase: Purchase) {
        orderUpdatedListener?.completeOrder(purchase, Product.ProductType.SUBSCRIPTION)
    }

    companion object {
        private const val TAG = "CustomerSales"
    }
}