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

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchaseHistoryResponseListener
import com.zuko.billingz.lib.LogUtil
import com.zuko.billingz.lib.store.client.Client
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Representation of a customer's order history, aka receipt or transaction history
 * @constructor
 * @param client
 */
class OrderHistory(private val client: Client) : History {

    private val mainScope = MainScope()
    private var activeSubscriptions: MutableList<Purchase> = mutableListOf()
    private var activeInAppProducts: MutableList<Purchase> = mutableListOf()

    private var isAlreadyQueried = false

    override fun getOwnedSubscriptions(): MutableList<Purchase> {
        return activeSubscriptions
    }

    override fun getOwnedInAppProducts(): MutableList<Purchase> {
        return activeInAppProducts
    }

    override fun refreshPurchaseHistory(sales: Sales) {
        LogUtil.log.v(TAG, "queryPurchases")
        if (isAlreadyQueried) {
            LogUtil.log.d(TAG, "Skipping purchase history refresh.")
            // skip - prevents double queries on initialization
            isAlreadyQueried = false
        } else {
            LogUtil.log.d(TAG, "Refreshing purchase history.")
            queryPurchases(sales)
            isAlreadyQueried = true
        }
    }

    override fun queryPurchases(sales: Sales) {
        LogUtil.log.v(TAG, "queryPurchases")
        if (client.isReady()) {
            LogUtil.log.i(TAG, "Fetching all purchases made by user.")

            mainScope.launch(Dispatchers.IO) {
                querySubscriptions(sales)
                queryInAppProducts(sales)
            }
        } else {
            LogUtil.log.e(TAG, "Android BillingClient was not ready yet to continue queryPurchases()")
        }
    }

    // todo - run async
    private fun querySubscriptions(sales: Sales) {
        LogUtil.log.v(TAG, "querySubscriptions")
        val subsResult = client.getBillingClient()?.queryPurchases(BillingClient.SkuType.SUBS)
        if (subsResult?.responseCode == BillingClient.BillingResponseCode.OK) { // todo verify
            subsResult.purchasesList?.let { subscriptions ->
                activeSubscriptions = subscriptions
                if (activeSubscriptions.isNotEmpty()) {
                    sales.processUpdatedPurchases(null, activeSubscriptions)
                    // queryPurchaseHistory(BillingClient.SkuType.SUBS)
                }

                LogUtil.log.i(TAG, "Subscription order history received: $subscriptions")
            } ?: LogUtil.log.d(TAG, "No subscription history available.")
        }
    }

    // todo - run async
    private fun queryInAppProducts(sales: Sales) {
        LogUtil.log.v(TAG, "queryInAppProducts")
        val inAppResult = client.getBillingClient()?.queryPurchases(BillingClient.SkuType.INAPP)
        if (inAppResult?.responseCode == BillingClient.BillingResponseCode.OK) { // todo verify
            inAppResult.purchasesList?.let { purchases ->
                activeInAppProducts = purchases
                if (activeInAppProducts.isNotEmpty())
                    sales.processUpdatedPurchases(null, activeSubscriptions)
                LogUtil.log.i(TAG, "In-app order history received: $purchases")
            } ?: LogUtil.log.d(TAG, "No In-app products history available.")
        }
    }

    // when to query history?
    override fun queryPurchaseHistory(skuType: String, listener: PurchaseHistoryResponseListener) {
        client.getBillingClient()?.queryPurchaseHistoryAsync(skuType, listener)
    }

    override fun destroy() {
        mainScope.cancel()
    }

    companion object {
        private const val TAG = "OrderHistory"
    }
}
