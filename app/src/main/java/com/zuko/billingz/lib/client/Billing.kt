package com.zuko.billingz.lib.client

import android.content.Context
import androidx.annotation.UiThread
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.PurchasesUpdatedListener
import com.zuko.billingz.lib.model.CleanUp

interface Billing: CleanUp {

    fun getBillingClient(): BillingClient?

    @UiThread
    fun initialized(): Boolean

    /**
     * @return Boolean
     * Checks if the client properly connected to the android billing api,
     * so that requests to other methods will succeed.
     */
    @UiThread
    fun isReady(): Boolean

    @UiThread
    fun initClient(context: Context?,
                   purchasesUpdatedListener: PurchasesUpdatedListener,
                   googlePlayConnectListener: GooglePlayConnectListener)
    @UiThread
    fun connect()

    @UiThread
    fun disconnect()

    fun checkConnection()

    interface GooglePlayConnectListener {
        fun connected()
    }

    interface GooglePlayReconnectListener {
        @UiThread
        fun retry()
        fun cancel()
    }
}