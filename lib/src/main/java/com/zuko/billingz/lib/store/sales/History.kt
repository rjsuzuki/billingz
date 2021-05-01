package com.zuko.billingz.lib.store.sales

import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchaseHistoryResponseListener
import com.zuko.billingz.lib.misc.CleanUp

interface History : CleanUp {

    /**
     *
     * Fetch all purchases to keep history up-to-date.
     * Network issues, multiple devices, and external purchases
     * could create untracked purchases - call this method in the
     * onCreate and onResume lifecycle events.
     */
    fun refreshPurchaseHistory(sales: Sales)

    /**
     * @param sales
     */
    fun queryPurchases(sales: Sales)

    /**
     * @param skuType
     * @param listener
     */
    fun queryPurchaseHistory(skuType: String, listener: PurchaseHistoryResponseListener)

    /**
     * @return - mutable list of active subscriptions
     */
    fun getOwnedSubscriptions(): MutableList<Purchase>

    /**
     * @return - mutable list of active in-app purchases
     */
    fun getOwnedInAppProducts(): MutableList<Purchase>
}
