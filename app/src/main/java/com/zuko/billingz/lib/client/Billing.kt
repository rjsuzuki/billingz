package com.zuko.billingz.lib.client

import android.content.Context
import androidx.annotation.UiThread
import androidx.lifecycle.MutableLiveData
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.PurchasesUpdatedListener
import com.zuko.billingz.lib.model.CleanUp

interface Billing: CleanUp {

    fun getBillingClient(): BillingClient?
    var isBillingClientReady: MutableLiveData<Boolean>

    /**
     * @return Boolean
     * Checks if the client has been initialized yet
     */
    @UiThread
    fun initialized(): Boolean

    /**
     * @return Boolean
     * Checks if the client properly connected to the android billing api,
     * so that requests to other methods will succeed.
     */
    @UiThread
    fun isReady(): Boolean

    /**
     * Initialize the Android Billing Library
     * INTERNAL USE ONLY
     * @param context
     * @param purchasesUpdatedListener
     * @param googlePlayConnectListener
     */
    @UiThread
    fun initClient(context: Context?,
                   purchasesUpdatedListener: PurchasesUpdatedListener,
                   googlePlayConnectListener: GooglePlayConnectListener)

    /**
     * Starts connection to GooglePlay
     * INTERNAL USE ONLY
     */
    @UiThread
    fun connect()

    /**
     * Stops connection to GooglePlay
     * INTERNAL USE ONLY
     */
    @UiThread
    fun disconnect()

    /**
     * Verifies connection to GooglePlay
     */
    fun checkConnection()

    /**
     * Callback used to respond to a successful connection.
     * INTERNAL USE ONLY
     */
    interface GooglePlayConnectListener {
        fun connected()
    }

    /**
     * Interface for reconnection logic to Google Play
     * INTERNAL USE ONLY
     */
    interface GooglePlayReconnectListener {
        @UiThread
        fun retry()
        fun cancel()
    }
}