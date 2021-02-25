package com.zuko.billingz.lib.client

import com.android.billingclient.api.Purchase

interface PurchaseNotification {

    /**
     * React to events of completed purchases that occur outside of the
     * normal purchase flow. i.e. purchases that are queried
     * from the purchaseHistory and are finally processed.
     */
    fun notifyUser(purchase: Purchase)
}