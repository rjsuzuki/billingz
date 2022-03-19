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
package com.zuko.billingz.amazon.store.client

import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.amazon.device.iap.PurchasingListener
import com.amazon.device.iap.PurchasingService
import com.amazon.device.iap.model.ProductDataResponse
import com.amazon.device.iap.model.PurchaseResponse
import com.amazon.device.iap.model.PurchaseUpdatesResponse
import com.amazon.device.iap.model.UserDataResponse
import com.zuko.billingz.amazon.store.inventory.AmazonInventoryz
import com.zuko.billingz.amazon.store.sales.AmazonSalez
import com.zuko.billingz.core.LogUtilz
import com.zuko.billingz.core.store.client.Clientz

class AmazonClient(val inventory: AmazonInventoryz, val sales: AmazonSalez) : Clientz {

    override var connectionState = MutableLiveData<Clientz.ConnectionStatus>()

    private var isInitialized = false
    private var isConnected = false
    private var userDataResponse: UserDataResponse? = null

    override fun initialized(): Boolean {
        return isInitialized
    }

    override fun isReady(): Boolean {
        return initialized() && isConnected
    }

    override fun init(context: Context?, connectionListener: Clientz.ConnectionListener) {
        LogUtilz.log.v(TAG, "initClient")
        PurchasingService.registerListener(
            context,
            object : PurchasingListener {

                override fun onUserDataResponse(response: UserDataResponse?) {
                    // Invoked after a call to getUserData().
                    // Determines the UserId and marketplace of the currently logged on user.
                    when (response?.requestStatus) {
                        UserDataResponse.RequestStatus.SUCCESSFUL -> {
                            LogUtilz.log.d(
                                TAG,
                                "Successful user data request: ${response.requestId}"
                            )
                            userDataResponse = response
                            isConnected = true
                        }
                        UserDataResponse.RequestStatus.FAILED -> {
                            LogUtilz.log.e(TAG, "Failed user data request: ${response.requestId}")
                            isConnected = false
                        }
                        UserDataResponse.RequestStatus.NOT_SUPPORTED -> {
                            isConnected = false
                            LogUtilz.log.wtf(
                                TAG,
                                "Unsupported user data request: ${response.requestId}"
                            )
                        }
                        else -> {
                            LogUtilz.log.w(
                                TAG,
                                "Unknown request status: ${response?.requestId}"
                            )
                        }
                    }
                }

                override fun onProductDataResponse(response: ProductDataResponse?) {
                    // Invoked after a call to getProductDataRequest(java.util.Set skus).
                    // Retrieves information about SKUs you would like to sell from your app.
                    // Use the valid SKUs in onPurchaseResponse().
                    inventory.handleQueriedProducts(response)
                }

                override fun onPurchaseResponse(response: PurchaseResponse?) {
                    // Invoked after a call to purchase(String sku).
                    // Used to determine the status of a purchase.
                    sales.handlePurchasedOrder(response)
                }

                override fun onPurchaseUpdatesResponse(response: PurchaseUpdatesResponse?) {
                    // Invoked after a call to getPurchaseUpdates(boolean reset).
                    // Retrieves the purchase history.
                    // Amazon recommends that you persist the returned PurchaseUpdatesResponse
                    // data and query the system only for updates.
                    sales.handleQueriedOrders(response)
                }
            }
        )
        isInitialized = true
    }

    override fun connect() {
        LogUtilz.log.v(TAG, "connect")
        // Call this method to retrieve the app-specific ID and marketplace for the user who is
        // currently logged on. For example, if a user switched accounts or if multiple users
        // accessed your app on the same device, this call will help you make sure that the receipts
        // that you retrieve are for the current user account.
        val userRequestId = PurchasingService.getUserData() // client
        LogUtilz.log.d(TAG, "Requesting user data: $userRequestId")
    }

    override fun disconnect() {
        LogUtilz.log.v(TAG, "disconnect")
    }

    override fun checkConnection() {
        LogUtilz.log.v(
            TAG,
            "checkConnection:" +
                "\nSDK_VERSION: ${PurchasingService.SDK_VERSION}" +
                "\nIS_SANDBOX_MODE: ${PurchasingService.IS_SANDBOX_MODE}"
        )
    }

    override fun destroy() {
        LogUtilz.log.v(TAG, "destroy")
    }

    companion object {
        private const val TAG = "AmazonClient"
    }
}
