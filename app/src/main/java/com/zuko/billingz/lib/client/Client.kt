package com.zuko.billingz.lib.client

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PurchasesUpdatedListener
import com.zuko.billingz.lib.LogUtil
import kotlinx.coroutines.*
import java.lang.Exception

/**
 * @author rjsuzuki
 */
class Client : Billing, Billing.GooglePlayReconnectListener {

    private val mainScope = MainScope()

    private var billingClient: BillingClient? = null
    private var isInitialized = false
    private var isConnected = false
    private var retryAttempts = 0
    private var maxAttempts = 3
    private var googlePlayConnectListener: Billing.GooglePlayConnectListener? = null

    override var isBillingClientReady = MutableLiveData<Boolean>()

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
                        isBillingClientReady.postValue(true)
                    }
                    else -> {
                        Log.w(TAG, "Unhandled response code: ${billingResult.responseCode}")
                        isConnected = false
                    }
                }
            }

            override fun onBillingServiceDisconnected() {
                isConnected = false
                retry()
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
        if(isInitialized && !isConnected) {
            connect()
        }
    }

    @Synchronized
    override fun retry() {
        if(isInitialized && !isConnected) {
            retryAttempts++
            if(retryAttempts <= maxAttempts) {
                val seconds = 5 * 1000L
                LogUtil.log.wtf(TAG, "Connection failed - Next conection attempt #$retryAttempts in $seconds seconds.")
                mainScope.launch(Dispatchers.IO) {
                    delay(seconds) //wait 5 seconds
                    connect()
                }
            }
        }
    }

    override fun cancel() {
        mainScope.cancel()
        retryAttempts = 0
    }

    override fun destroy() {
        isInitialized = false
        disconnect()
        cancel()
    }

    companion object {
        private const val TAG = "Client"
    }
}