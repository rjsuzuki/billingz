package com.zuko.billingz.lib.sales

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase
import com.zuko.billingz.lib.LogUtil
import com.zuko.billingz.lib.client.Billing
import com.zuko.billingz.lib.inventory.Inventory

class OrderHistory(val client: Billing, val inventory: Inventory): History {

    private var ownedSubscriptions: MutableList<Purchase> = mutableListOf()
    private var ownedInAppProducts: MutableList<Purchase> = mutableListOf()

    private var isAlreadyQueried = false

    override fun getInAppProductsHistory(): MutableList<Purchase> {
        return ownedInAppProducts
    }

    override fun getSubscriptionHistory(): MutableList<Purchase> {
        return ownedSubscriptions
    }

    override fun refreshPurchaseHistory(isOnCreateEvent: Boolean) {
        if(isOnCreateEvent) {
            queryPurchases()
            isAlreadyQueried = true
        } else if(isAlreadyQueried) {
            // skip - prevents double queries on initialization
            isAlreadyQueried = false
        } else {
            queryPurchases()
        }
    }
    //todo - get Purchase retrieved from BillingClient - queryPurchases
    override fun queryPurchases() {
        if(client.isReady()) {
            LogUtil.log.i(TAG, "Fetching all purchases made by user.")

            //todo - background thread async
            val inAppResult = client.getBillingClient()?.queryPurchases(BillingClient.SkuType.INAPP)
            if(inAppResult?.responseCode == BillingClient.BillingResponseCode.OK) {
                inAppResult.purchasesList?.let { purchases ->
                    ownedInAppProducts = purchases
                    LogUtil.log.i(TAG, "In-app order history received: $purchases")
                } ?: LogUtil.log.d(TAG, "No In-app products history available.")
            }

            //todo - background thread async
            val subsResult =  client.getBillingClient()?.queryPurchases(BillingClient.SkuType.SUBS)
            if(subsResult?.responseCode == BillingClient.BillingResponseCode.OK) {
                subsResult.purchasesList?.let { subscriptions ->
                    ownedSubscriptions = subscriptions
                    LogUtil.log.i(TAG, "Subscription order history received: $subscriptions")
                } ?: LogUtil.log.d(TAG, "No subscription history available.")
            }
        } else {
            LogUtil.log.e(TAG, "Android BillingClient was not ready yet to continue queryPurchases()")
        }
    }

    private suspend fun querySubscriptions() {

    }

    private suspend fun queryInAppProducts() {

    }

    //todo - complete the purchases?


    companion object {
        private const val TAG = "OrderHistory"
    }
}