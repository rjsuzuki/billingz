package com.zuko.billingz.lib.client

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PurchasesUpdatedListener
import com.zuko.billingz.lib.model.PurchaseWrapper
import com.zuko.billingz.lib.model.Result
import com.zuko.billingz.lib.sales.Order

/**
 * @author rjsuzuki
 */
class Client (val context: Context) : Billing {

    override var order: MutableLiveData<Order> = MutableLiveData()

    private var billingClient: BillingClient? = null
    private var isConnected = false

    override fun getBillingClient(): BillingClient? {
        return billingClient
    }

    /**
     * @return Boolean
     * Checks if the client both initialized and is currently connected to the service,
     * so that requests to other methods will succeed.
     */
    override fun isReady(): Boolean {
        return isConnected && billingClient?.isReady == true
    }

    /*****************************************************************************************************
     * Initialization
     *****************************************************************************************************/


    override fun initClient(context: Context?, listener: PurchasesUpdatedListener) {
        if(billingClient != null) {
            billingClient?.endConnection() //will this throw an error?
            billingClient = null
        }
        context?.let {
            billingClient = BillingClient.newBuilder(context)
                .setListener(listener)
                .enablePendingPurchases() //switch
                .build()
        }
    }

    override fun connect() {
        billingClient?.startConnection(object: BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                when(billingResult.responseCode) {
                    BillingClient.BillingResponseCode.OK -> {
                        // The BillingClient is ready. You can query purchases here.
                        isConnected = true
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
        TODO("Not yet implemented")
    }

    override fun error(billingResult: BillingResult?) {
        TODO("Not yet implemented")
    }

    companion object {
        private const val TAG = "Client"
    }
}