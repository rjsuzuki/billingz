package com.zuko.billingz.lib.sales

import com.android.billingclient.api.Purchase

interface History {

    /**
     *
     * Fetch all purchases to keep history up-to-date.
     * Network issues, multiple devices, and external purchases
     * could create untracked purchases - call this method in the
     * onCreate and onResume lifecycle events.
     */
    fun refreshPurchaseHistory(isOnCreateEvent: Boolean)


    fun queryPurchases()

    fun getSubscriptionHistory() : MutableList<Purchase>
    fun getInAppProductsHistory() : MutableList<Purchase>
}