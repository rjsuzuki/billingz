/*
 * Copyright 2021 rjsuzuki
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.zuko.billingz.google.store.client

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PurchasesUpdatedListener
import com.zuko.billingz.core.LogUtilz
import com.zuko.billingz.core.store.client.Clientz
import com.zuko.billingz.google.store.sales.GoogleResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GoogleClient(private val purchasesUpdatedListener: PurchasesUpdatedListener) : Clientz {

    private val mainScope = MainScope()

    private var billingClient: BillingClient? = null
    private var isInitialized = false
    private var isConnected = false
    private var retryAttempts = 0
    private var maxAttempts = 3
    private var connectionListener: Clientz.ConnectionListener? = null

    override var isClientReady = MutableLiveData<Boolean>()
        get() {
            field.value = isReady()
            return field
        }

    fun getBillingClient(): BillingClient? {
        return billingClient
    }

    override fun initialized(): Boolean {
        return isInitialized
    }

    override fun isReady(): Boolean {
        LogUtilz.log.d(TAG, "Connection state: ${getConnectionState()}")
        return billingClient?.isReady == true
    }

    override fun init(
        context: Context?,
        connectionListener: Clientz.ConnectionListener
    ) {
        LogUtilz.log.v(TAG, "Initializing client...")
        this.connectionListener = connectionListener
        try {
            if (billingClient != null) {
                billingClient?.endConnection()
                billingClient = null
                isInitialized = false
                isConnected = false
                isClientReady.postValue(false)
            }
            context?.let {
                billingClient = BillingClient.newBuilder(context)
                    .setListener(purchasesUpdatedListener)
                    .enablePendingPurchases() // switch
                    .build()
                isInitialized = true
            } ?: LogUtilz.log.w(TAG, "Failed to build client: null context")
        } catch (e: Exception) {
            isClientReady.postValue(false)
            LogUtilz.log.wtf(TAG, "Failed to instantiate Android BillingClient. ${e.localizedMessage}")
        }
    }

    override fun connect() {
        LogUtilz.log.v(TAG, "Connecting to Google...")
        if (billingClient?.isReady == true) {
            LogUtilz.log.v(TAG, "Client is already connected to Google...")
            return
        }
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                GoogleResponse.logResult(billingResult)
                when (billingResult.responseCode) {
                    BillingClient.BillingResponseCode.OK -> {
                        // The BillingClient is ready. You can query purchases here.
                        isConnected = true
                        connectionListener?.connected()
                        isClientReady.postValue(true)
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
        LogUtilz.log.v(TAG, "Disconnecting from Google...")
        billingClient?.endConnection()
    }

    override fun checkConnection() {
        LogUtilz.log.d(TAG, "Connection state: ${getConnectionState()}")
        if (!isReady()) {
            connect()
        }
    }

    private fun getConnectionState(): String {
        return ConnectionStatus.values()[billingClient?.connectionState ?: 0].name
    }

    enum class ConnectionStatus {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        CLOSED
    }

    @Synchronized
    internal fun retry() {
        LogUtilz.log.w(TAG, "Retrying to connect...")
        if (isInitialized && !isConnected) {
            retryAttempts++
            if (retryAttempts <= maxAttempts) {
                val seconds = 5 * 1000L
                LogUtilz.log.wtf(TAG, "Connection failed - Next conection attempt #$retryAttempts in $seconds seconds.")
                mainScope.launch(Dispatchers.IO) {
                    delay(seconds) // wait 5 seconds
                    connect()
                }
            }
        }
    }

    private fun cancel() {
        mainScope.cancel()
        retryAttempts = 0
    }

    override fun destroy() {
        LogUtilz.log.v(TAG, "Destroying client...")
        isInitialized = false
        disconnect()
        cancel()
    }

    companion object {
        private const val TAG = "BillingzGoogleClient"
    }
}
