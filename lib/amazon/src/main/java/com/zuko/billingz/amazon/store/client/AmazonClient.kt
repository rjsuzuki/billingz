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
package com.zuko.billingz.amazon.store.client

import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.amazon.device.drm.LicensingService
import com.amazon.device.iap.PurchasingListener
import com.amazon.device.iap.PurchasingService
import com.amazon.device.iap.model.ProductDataResponse
import com.amazon.device.iap.model.PurchaseResponse
import com.amazon.device.iap.model.PurchaseUpdatesResponse
import com.amazon.device.iap.model.UserDataResponse
import com.zuko.billingz.amazon.store.inventory.AmazonInventoryz
import com.zuko.billingz.amazon.store.sales.AmazonSalez
import com.zuko.billingz.core.misc.Logger
import com.zuko.billingz.core.store.client.Clientz

class AmazonClient(val inventory: AmazonInventoryz, val sales: AmazonSalez) : AmazonClientz {

    private var isInitialized = false
    private var isConnected = false
    private var userDataResponse: UserDataResponse? = null

    private var context: Context? = null

    override var connectionState = MutableLiveData<Clientz.ConnectionStatus>()

    override fun initialized(): Boolean {
        return isInitialized
    }

    override fun isReady(): Boolean {
        return initialized() && isConnected && connectionState.value == Clientz.ConnectionStatus.CONNECTED
    }

    override fun init(context: Context?, connectionListener: Clientz.ConnectionListener) {
        Logger.v(TAG, "Initializing AmazonClient...")
        this.context = context
        try {
            LicensingService.verifyLicense(context) { response ->
                Logger.d(TAG, "License Status: ${response.requestStatus.name}")
            }
            isInitialized = true
        } catch (e: Exception) {
            Logger.e(TAG, e)
        }
    }

    override fun connect() {
        Logger.v(TAG, "Connecting to Amazon IAP...")
        connectionState.postValue(Clientz.ConnectionStatus.CONNECTING)
        registerPurchasingListener()
    }

    fun pause() {
        Logger.v(TAG, "Pausing...")
        connectionState.postValue(Clientz.ConnectionStatus.CLOSED)
    }

    private fun requestUserData() {
        Logger.v(TAG, "Requesting user data...")
        // Call this method to retrieve the app-specific ID and marketplace for the user who is
        // currently logged on. For example, if a user switched accounts or if multiple users
        // accessed your app on the same device, this call will help you make sure that the receipts
        // that you retrieve are for the current user account.
        try {
            val userRequestId = PurchasingService.getUserData() // client
            Logger.d(TAG, "UserData request id: $userRequestId")
        } catch (e: Exception) {
            Logger.e(TAG, e)
        }
    }

    private fun registerPurchasingListener() {
        Logger.v(TAG, "registerListener")
        val purchasingListener = object : PurchasingListener {
            override fun onUserDataResponse(response: UserDataResponse?) {
                Logger.d(TAG, "onPurchaseUpdatesResponse")
                // Invoked after a call to getUserData().
                // Determines the UserId and marketplace of the currently logged on user.
                when (response?.requestStatus) {
                    UserDataResponse.RequestStatus.SUCCESSFUL -> {
                        Logger.d(
                            TAG,
                            "Successful user data request: ${response.requestId}" +
                                "\nmarketplace: ${response.userData?.marketplace}"
                        )
                        userDataResponse = response
                        isConnected = true
                        connectionState.postValue(Clientz.ConnectionStatus.CONNECTED)
                    }
                    UserDataResponse.RequestStatus.FAILED -> {
                        Logger.e(TAG, "Failed user data request: ${response.requestId}")
                        isConnected = false
                        connectionState.postValue(Clientz.ConnectionStatus.DISCONNECTED)
                    }
                    UserDataResponse.RequestStatus.NOT_SUPPORTED -> {
                        Logger.wtf(
                            TAG,
                            "Unsupported user data request: ${response.requestId}"
                        )
                    }
                    else -> {
                        Logger.w(
                            TAG,
                            "Unknown request status: ${response?.requestId}"
                        )
                    }
                }
            }

            override fun onProductDataResponse(response: ProductDataResponse?) {
                Logger.d(TAG, "onProductDataResponse")
                // Invoked after a call to getProductDataRequest(java.util.Set skus).
                // Retrieves information about SKUs you would like to sell from your app.
                // Use the valid SKUs in onPurchaseResponse().
                inventory.processQueriedProducts(response)
            }

            override fun onPurchaseResponse(response: PurchaseResponse?) {
                Logger.d(TAG, "onPurchaseResponse")
                // Invoked after a call to purchase(String sku).
                // Used to determine the status of a purchase.
                sales.processPurchase(response)
            }

            override fun onPurchaseUpdatesResponse(response: PurchaseUpdatesResponse?) {
                Logger.d(TAG, "onPurchaseUpdatesResponse")
                // Invoked after a call to getPurchaseUpdates(boolean reset).
                // Retrieves the purchase history.
                // Amazon recommends that you persist the returned PurchaseUpdatesResponse
                // data and query the system only for updates.
                sales.processPurchaseUpdates(response)
            }
        }
        try {
            PurchasingService.registerListener(
                context,
                purchasingListener
            )
        } catch (e: Exception) {
            Logger.e(TAG, e)
        }
    }

    override fun disconnect() {
        Logger.v(TAG, "disconnecting from Amazon IAP")
        isConnected = false
        connectionState.postValue(Clientz.ConnectionStatus.DISCONNECTED)
    }

    override fun checkConnection() {
        Logger.v(
            TAG,
            "checkConnection:" +
                "\nSDK_VERSION: ${PurchasingService.SDK_VERSION}" +
                "\nSDK_MODE: ${LicensingService.getAppstoreSDKMode()}"
        )
        if (!isReady()) {
            requestUserData()
        }
    }

    override fun destroy() {
        Logger.v(TAG, "Destroying client...")
        isInitialized = false
        disconnect()
    }

    companion object {
        private const val TAG = "AmazonClient"
    }
}
