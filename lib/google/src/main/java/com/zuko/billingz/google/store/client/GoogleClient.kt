/*
 *
 *  * Copyright 2021 rjsuzuki
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  *
 *
 */
package com.zuko.billingz.google.store.client

import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PurchasesUpdatedListener
import com.zuko.billingz.core.misc.Logger
import com.zuko.billingz.core.store.client.Clientz
import com.zuko.billingz.google.BuildConfig
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

    override var connectionState = MutableLiveData<Clientz.ConnectionStatus>()
        get() {
            field.postValue(getConnectionState())
            return field
        }

    fun getBillingClient(): BillingClient? {
        return billingClient
    }

    override fun initialized(): Boolean {
        return isInitialized
    }

    override fun isReady(): Boolean {
        Logger.d(TAG, "Is ready connection state: ${getConnectionStateName()}")
        return isInitialized && isConnected && billingClient?.isReady == true
    }

    override fun init(
        context: Context?,
        connectionListener: Clientz.ConnectionListener
    ) {
        Logger.v(
            TAG, "Initializing client..." +
                    "\n debug: ${BuildConfig.DEBUG}" +
                    "\n build: ${BuildConfig.BUILD_TYPE}" +
                    "\n version: ${BuildConfig.VERSION}"
        )
        this.connectionListener = connectionListener
        try {
            if (billingClient != null) {
                Logger.v(TAG, "Client already initialized...")
                connect()
                isInitialized = true
                return
            }
            context?.let {
                billingClient = BillingClient.newBuilder(context)
                    .setListener(purchasesUpdatedListener)
                    .enablePendingPurchases() // switch
                    .build()
                isInitialized = true
            } ?: Logger.w(TAG, "Failed to build client: null context")
        } catch (e: Exception) {
            connectionState.postValue(getConnectionState())
            Logger.wtf(TAG, "Failed to instantiate Android BillingClient. ${e.localizedMessage}")
        }
    }

    override fun connect() {
        Logger.v(TAG, "Connecting to Google...")
        if (billingClient?.isReady == true) {
            isConnected = true
            connectionListener?.connected()
            connectionState.postValue(getConnectionState())
            Logger.v(TAG, "Client is already connected to Google...")
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
                        connectionState.postValue(getConnectionState())
                    }
                    else -> {
                        Logger.w(TAG, "Unhandled response code: ${billingResult.responseCode}")
                        isConnected = false
                        connectionState.postValue(getConnectionState())
                    }
                }
            }

            override fun onBillingServiceDisconnected() {
                connectionState.postValue(getConnectionState())
                isConnected = false
                retry()
                // Note: It's strongly recommended that you implement your own connection retry logic
                // and override the onBillingServiceDisconnected() method.
                // Make sure you maintain the BillingClient connection when executing any methods.
            }
        })
    }

    override fun disconnect() {
        Logger.v(TAG, "Disconnecting from Google...")
        billingClient?.endConnection()
        isConnected = false
    }

    override fun checkConnection() {
        Logger.d(TAG, "Connection state: ${getConnectionStateName()}")
        if (!isReady()) {
            connect()
        }
    }

    internal fun getConnectionState(): Clientz.ConnectionStatus {
        return Clientz.ConnectionStatus.entries[billingClient?.connectionState ?: 0]
    }

    private fun getConnectionStateName(): String {
        return getConnectionState().name
    }

    @Synchronized
    internal fun retry() {
        Logger.w(TAG, "Retrying to connect...")
        if (isInitialized && !isConnected) {
            retryAttempts++
            if (retryAttempts <= maxAttempts) {
                val seconds = 5 * 1000L
                Logger.wtf(TAG, "Connection failed - Next conection attempt #$retryAttempts in $seconds seconds.")
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
        Logger.v(TAG, "Destroying client...")
        isInitialized = false
        disconnect()
        cancel()
    }

    companion object {
        private const val TAG = "BillingzGoogleClient"
    }
}
