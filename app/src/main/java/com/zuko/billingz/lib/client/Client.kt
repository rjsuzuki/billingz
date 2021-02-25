package com.zuko.billingz.lib.client

import android.content.Context
import android.util.Log
import androidx.annotation.UiThread
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PurchasesUpdatedListener
import com.zuko.billingz.lib.LogUtil
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import java.lang.Exception

/**
 * @author rjsuzuki
 */
class Client : Billing, Billing.GooglePlayReconnectListener {

    private val mainScope = MainScope()

    private var billingClient: BillingClient? = null
    private var isInitialized = false
    private var isConnected = false

    private var googlePlayConnectListener: Billing.GooglePlayConnectListener? = null

    override fun getBillingClient(): BillingClient? {
        return billingClient
    }

    override fun initialized(): Boolean {
        return isInitialized
    }

    override fun isReady(): Boolean {
        return initialized() && isConnected && billingClient?.isReady == true
    }

    /*****************************************************************************************************
     * Initialization
     *****************************************************************************************************/

    override fun initClient(context: Context?,
                            purchasesUpdatedListener: PurchasesUpdatedListener,
                            googlePlayConnectListener: Billing.GooglePlayConnectListener) {

        this.googlePlayConnectListener = googlePlayConnectListener

        try {
            if(billingClient != null) {
                billingClient?.endConnection()
                billingClient = null
                isInitialized = false
            }
            context?.let {
                billingClient = BillingClient.newBuilder(context)
                    .setListener(purchasesUpdatedListener)
                    .enablePendingPurchases() //switch
                    .build()
                isInitialized = true
            }
        } catch (e: Exception) {
            LogUtil.log.wtf(TAG, "Failed to instantiate Android BillingClient. ${e.localizedMessage}")
        }
    }

    override fun connect() {
        billingClient?.startConnection(object: BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                when(billingResult.responseCode) {
                    BillingClient.BillingResponseCode.OK -> {
                        // The BillingClient is ready. You can query purchases here.
                        isConnected = true
                        googlePlayConnectListener?.connected()
                        //isReadyLiveData.postValue(true)
                        //initMockData()
                    }
                    else -> {
                        Log.w(TAG, "Unhandled response code: ${billingResult.responseCode}")
                        isConnected = false
                    }
                }
            }

            override fun onBillingServiceDisconnected() {
                isConnected = false
                //isReadyLiveData.postValue(false)
                // Note: It's strongly recommended that you implement your own connection retry logic
                // and override the onBillingServiceDisconnected() method.
                // Make sure you maintain the BillingClient connection when executing any methods.
            }
        })
    }

    override fun disconnect() {
        billingClient?.endConnection()
    }

    override fun checkConnection() {
        TODO("Not yet implemented")
    }

    override fun retry() {
        TODO("Not yet implemented")
    }

    override fun cancel() {
        TODO("Not yet implemented")
    }

    override fun destroy() {
        isInitialized = false
        disconnect()
        mainScope.cancel()
    }

    companion object {
        private const val TAG = "Client"
    }
}