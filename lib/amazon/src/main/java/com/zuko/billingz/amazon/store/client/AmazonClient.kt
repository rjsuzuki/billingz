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
import androidx.collection.ArrayMap
import androidx.lifecycle.MutableLiveData
import com.amazon.device.iap.PurchasingListener
import com.amazon.device.iap.PurchasingService
import com.amazon.device.iap.model.ProductDataResponse
import com.amazon.device.iap.model.PurchaseResponse
import com.amazon.device.iap.model.PurchaseUpdatesResponse
import com.amazon.device.iap.model.UserDataResponse
import com.zuko.billingz.amazon.store.model.AmazonOrder
import com.zuko.billingz.amazon.store.model.AmazonProduct
import com.zuko.billingz.amazon.store.model.AmazonReceipt
import com.zuko.billingz.core.LogUtilz
import com.zuko.billingz.core.store.client.Clientz
import com.zuko.billingz.core.store.inventory.Inventoryz
import com.zuko.billingz.core.store.model.Productz
import com.zuko.billingz.core.store.model.Receiptz
import com.zuko.billingz.core.store.sales.Salez

class AmazonClient(val inventory: Inventoryz, val sales: Salez) : Clientz {

    override var isClientReady = MutableLiveData<Boolean>()

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
                    }
                }

                override fun onProductDataResponse(response: ProductDataResponse?) {
                    // Invoked after a call to getProductDataRequest(java.util.Set skus).
                    // Retrieves information about SKUs you would like to sell from your app.
                    // Use the valid SKUs in onPurchaseResponse().
                    when (response?.requestStatus) {
                        ProductDataResponse.RequestStatus.SUCCESSFUL -> {
                            LogUtilz.log.d(
                                TAG,
                                "Successful product data request: ${response.requestId}"
                            )

                            // convert
                            val products = ArrayMap<String, Productz>()
                            for (r in response.productData) {
                                val product = AmazonProduct(r.value)
                                products[r.key] = product
                                LogUtilz.log.v(TAG, "Validated product: $product")
                            }
                            // todo inventory.allProducts = products

                            // cache
                            val unavailableSkusSet = response.unavailableSkus // todo
                        }
                        ProductDataResponse.RequestStatus.FAILED -> {
                            LogUtilz.log.e(
                                TAG,
                                "Failed product data request: ${response.requestId}"
                            )
                        }
                        ProductDataResponse.RequestStatus.NOT_SUPPORTED -> {
                            LogUtilz.log.wtf(
                                TAG,
                                "Unsupported product data request: ${response.requestId}"
                            )
                        }
                    }
                }

                override fun onPurchaseResponse(response: PurchaseResponse?) {
                    // Invoked after a call to purchase(String sku).
                    // Used to determine the status of a purchase.
                    when (response?.requestStatus) {
                        PurchaseResponse.RequestStatus.SUCCESSFUL -> {
                            LogUtilz.log.d(
                                TAG,
                                "Successful purchase request: ${response.requestId}"
                            )
                            // convert to order
                            val order = AmazonOrder(response)
                            sales.validateOrder(order)
                        }
                        PurchaseResponse.RequestStatus.FAILED -> {
                            LogUtilz.log.e(TAG, "Failed purchase request: ${response.requestId}")
                            val order = AmazonOrder(response)
                            sales.failedOrder(order)
                        }
                        PurchaseResponse.RequestStatus.ALREADY_PURCHASED -> {
                            LogUtilz.log.w(
                                TAG,
                                "Already purchased product for purchase request: ${response.requestId}"
                            )
                            val order = AmazonOrder(response)
                            sales.failedOrder(order)
                        }
                        PurchaseResponse.RequestStatus.INVALID_SKU -> {
                            LogUtilz.log.w(
                                TAG,
                                "Invalid sku id for purchase request: ${response.requestId}"
                            )
                            val order = AmazonOrder(response)
                            sales.failedOrder(order)
                        }
                        PurchaseResponse.RequestStatus.NOT_SUPPORTED -> {
                            LogUtilz.log.wtf(
                                TAG,
                                "Unsupported purchase request: ${response.requestId}"
                            )
                            val order = AmazonOrder(response)
                            sales.failedOrder(order)
                        }
                    }
                }

                override fun onPurchaseUpdatesResponse(response: PurchaseUpdatesResponse?) {
                    // Invoked after a call to getPurchaseUpdates(boolean reset).
                    // Retrieves the purchase history.
                    // Amazon recommends that you persist the returned PurchaseUpdatesResponse
                    // data and query the system only for updates.
                    when (response?.requestStatus) {
                        PurchaseUpdatesResponse.RequestStatus.SUCCESSFUL -> {
                            LogUtilz.log.d(
                                TAG,
                                "Successful purchase updates request: ${response.requestId}"
                            )

                            // convert receipts
                            val map = ArrayMap<String, Receiptz>()
                            for (r in response.receipts) {
                                val receipt = AmazonReceipt(r)
                                receipt.userId = response.userData.userId
                                receipt.marketplace = response.userData.marketplace
                                map[r.receiptId] = receipt
                            }
                            sales.orderHistory.value = map
                        }
                        PurchaseUpdatesResponse.RequestStatus.FAILED -> {
                            LogUtilz.log.e(
                                TAG,
                                "Failed purchase updates request: ${response.requestId}"
                            )
                        }
                        PurchaseUpdatesResponse.RequestStatus.NOT_SUPPORTED -> {
                            LogUtilz.log.wtf(
                                TAG,
                                "Unsupported purchase update request: ${response.requestId}"
                            )
                        }
                    }
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
        LogUtilz.log.v(TAG, "checkConnection")
    }

    override fun destroy() {
        LogUtilz.log.v(TAG, "destroy")
    }

    companion object {
        private const val TAG = "AmazonClient"
    }
}
