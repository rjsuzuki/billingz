package com.zuko.billingz.lib.sales

import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchaseHistoryResponseListener
import com.zuko.billingz.lib.model.CleanUp

interface History: CleanUp {

    /**
     *
     * Fetch all purchases to keep history up-to-date.
     * Network issues, multiple devices, and external purchases
     * could create untracked purchases - call this method in the
     * onCreate and onResume lifecycle events.
     */
    fun refreshPurchaseHistory(sales: Sales)
    fun queryPurchases(sales: Sales)
    fun queryPurchaseHistory(skuType: String, listener: PurchaseHistoryResponseListener)

    fun getOwnedSubscriptions() : MutableList<Purchase>
    fun getOwnedInAppProducts() : MutableList<Purchase>
}