package com.zuko.billingz

import android.app.Activity
import com.android.billingclient.api.Purchase

interface IBillingManager {
    fun purchase(activity: Activity?, productId: String?)


    fun processConsumable(purchase: Purchase)
    fun processNonConsumable(purchase: Purchase)
    fun processSubscription(purchase: Purchase)

    //fun getAllProducts()
    //fun getFreeProducts()
    //fun getSubscriptions()
    //fun getConsumables()
    //fun getPurchaseHistory()
    //fun getPromotion()

    /**
     * Purchases can be made outside of app, or finish while app is in background.
     * show in-app popup, or deliver msg to an inbox, or use an OS notification
     */
   // fun notifyPurchase()


    //fun enablePendingTransactionSupport()
}