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
package com.zuko.billingz.amazon.store.sales

import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.collection.ArrayMap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.amazon.device.iap.PurchasingService
import com.amazon.device.iap.model.FulfillmentResult
import com.amazon.device.iap.model.PurchaseResponse
import com.amazon.device.iap.model.PurchaseUpdatesResponse
import com.zuko.billingz.amazon.store.model.AmazonOrder
import com.zuko.billingz.amazon.store.model.AmazonReceipt
import com.zuko.billingz.core.LogUtilz
import com.zuko.billingz.core.store.client.Clientz
import com.zuko.billingz.core.store.model.Orderz
import com.zuko.billingz.core.store.model.Productz
import com.zuko.billingz.core.store.model.Receiptz
import com.zuko.billingz.core.store.sales.Salez
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel

// https://developer.amazon.com/docs/in-app-purchasing/iap-implement-iap.html#responsereceiver
class AmazonSales : AmazonSalez {

    private val mainScope = MainScope()

    override var currentReceipt = MutableLiveData<Receiptz>()
    private var queriedOrders = MutableLiveData<Orderz>()

    override var orderHistory: MutableLiveData<ArrayMap<String, Receiptz>> = MutableLiveData()
    override var orderUpdaterListener: Salez.OrderUpdaterListener? = null
    override var orderValidatorListener: Salez.OrderValidatorListener? = null

    private val validatorCallback: Salez.ValidatorCallback = object : Salez.ValidatorCallback {
        override fun validated(order: Orderz) {
            // verify amazon receipt
            processOrder(order)
        }

        override fun invalidated(order: Orderz) {
            Log.d(TAG, "onFailure")
            cancelOrder(order)
        }
    }

    private val updaterCallback: Salez.UpdaterCallback = object : Salez.UpdaterCallback {

        override fun complete(order: Orderz) {
            // fulfill order
            completeOrder(order)
        }

        override fun cancel(order: Orderz) {
            cancelOrder(order)
        }
    }

    // step 1
    override fun startOrder(
        activity: Activity?,
        product: Productz,
        client: Clientz,
        options: Bundle?
    ) {
        PurchasingService.purchase(product.sku)
    }

    override fun handlePurchasedOrder(response: PurchaseResponse?) {
        when (response?.requestStatus) {
            PurchaseResponse.RequestStatus.SUCCESSFUL -> {
                LogUtilz.log.d(
                    TAG,
                    "Successful purchase request: ${response.requestId}"
                )
                // convert to order
                val order = AmazonOrder(response)
                validateOrder(order)
            }
            PurchaseResponse.RequestStatus.FAILED -> {
                LogUtilz.log.e(TAG, "Failed purchase request: ${response.requestId}")
                val order = AmazonOrder(response)
                failedOrder(order)
            }
            PurchaseResponse.RequestStatus.ALREADY_PURCHASED -> {
                LogUtilz.log.w(
                    TAG,
                    "Already purchased product for purchase request: ${response.requestId}"
                )
                val order = AmazonOrder(response)
                failedOrder(order)
            }
            PurchaseResponse.RequestStatus.INVALID_SKU -> {
                LogUtilz.log.w(
                    TAG,
                    "Invalid sku id for purchase request: ${response.requestId}"
                )
                val order = AmazonOrder(response)
                failedOrder(order)
            }
            PurchaseResponse.RequestStatus.NOT_SUPPORTED -> {
                LogUtilz.log.wtf(
                    TAG,
                    "Unsupported purchase request: ${response.requestId}"
                )
                val order = AmazonOrder(response)
                failedOrder(order)
            }
            else -> {
                LogUtilz.log.w(
                    TAG,
                    "Unknown request status: ${response?.requestId}"
                )
            }
        }
    }

    // step 2
    override fun validateOrder(order: Orderz) {
        order.state = Orderz.State.VALIDATING

        try {
            if (order is AmazonOrder) {
                if (order.response.receipt?.isCanceled == true) {
                    // revoke
                    cancelOrder(order)
                    Log.wtf(TAG, "isCanceled")
                    return
                }
                // Verify the receipts from the purchase by having your back-end server
                // verify the receiptId with Amazon's Receipt Verification Service (RVS) before fulfilling the item
                orderValidatorListener?.validate(order, validatorCallback) ?: LogUtilz.log.e(
                    TAG,
                    "Null validator object. Cannot complete order."
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, e.localizedMessage ?: "error")
        }
    }

    // step 3
    override fun processOrder(order: Orderz) {
        order.state = Orderz.State.PROCESSING
        completeOrder(order)
    }

    // step 4
    override fun completeOrder(order: Orderz) {
        try {
            if (order is AmazonOrder) {

                // we check if the order is canceled again before completing
                if (order.response.receipt?.isCanceled == true) {
                    // revoke
                    cancelOrder(order)
                    Log.wtf(TAG, "isCanceled")
                    return
                }

                when (order.product?.type) {
                    Productz.Type.CONSUMABLE -> completeConsumable(order.response)
                    Productz.Type.NON_CONSUMABLE -> completeNonConsumable(order.response)
                    Productz.Type.SUBSCRIPTION -> completeSubscription(order.response)
                    else -> {}
                }
                // successful
                PurchasingService.notifyFulfillment(order.response.receipt.receiptId, FulfillmentResult.FULFILLED)
                // update history
                refreshQueries()
            }
        } catch (e: Exception) {
            Log.e(TAG, e.localizedMessage ?: "error")
        }
    }

    override fun refreshQueries() {
        val purchaseUpdatesRequestId = PurchasingService.getPurchaseUpdates(false)
        Log.d(TAG, "Refresh receipts: $purchaseUpdatesRequestId")
    }

    override fun queryOrders(): LiveData<Orderz> {
        val purchaseUpdatesRequestId = PurchasingService.getPurchaseUpdates(true)
        Log.d(TAG, "queryOrders \npurchaseUpdatesRequestId: $purchaseUpdatesRequestId")
        // todo - if order is pending still
        // orderUpdaterListener?.onResume(order, updaterCallback)

        // retrieves all Subscription and Entitlement purchases across all devices.
        // A consumable purchase can be retrieved only from the device where it was purchased.
        // getPurchaseUpdates
        // retrieves only unfulfilled and cancelled consumable purchases. Amazon recommends that you
        // persist the returned PurchaseUpdatesResponse data and query the system only for updates.
        // The response is paginated.
        return queriedOrders
    }

    override fun queryReceipts(type: Productz.Type?) {
        val purchaseUpdatesRequestId = PurchasingService.getPurchaseUpdates(true) // sales
        LogUtilz.log.d(TAG, "queryReceipts: \npurchaseUpdatesRequestId: $purchaseUpdatesRequestId")
    }

    override fun handleQueriedOrders(response: PurchaseUpdatesResponse?) {
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
                orderHistory.value = map
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
            else -> {
                LogUtilz.log.w(
                    TAG,
                    "Unknown request status: ${response?.requestId}"
                )
            }
        }
    }

    override fun setObfuscatedIdentifiers(accountId: String?, profileId: String?) {
        LogUtilz.log.w(TAG, "setObfuscatedIdentifiers: is not supported by Amazon IAP.")
    }

    private fun completeConsumable(response: PurchaseResponse) {
        LogUtilz.log.v(TAG, "completeConsumable")
        // convert receipt to AmazonReceipt
        val amazonReceipt = AmazonReceipt(response.receipt)
        currentReceipt.postValue(amazonReceipt)
        orderUpdaterListener?.onComplete(amazonReceipt)
    }

    private fun completeNonConsumable(response: PurchaseResponse) {
        LogUtilz.log.v(TAG, "completeNonConsumable")
        val amazonReceipt = AmazonReceipt(response.receipt)
        currentReceipt.postValue(amazonReceipt)
        orderUpdaterListener?.onComplete(amazonReceipt)
    }

    private fun completeSubscription(response: PurchaseResponse) {
        LogUtilz.log.v(TAG, "completeSubscription")
        val amazonReceipt = AmazonReceipt(response.receipt)
        currentReceipt.postValue(amazonReceipt)
        orderUpdaterListener?.onComplete(amazonReceipt)
    }

    override fun cancelOrder(order: Orderz) {
        LogUtilz.log.v(TAG, "cancelOrder")
        if (order is AmazonOrder) {
            PurchasingService.notifyFulfillment(
                order.response.receipt.receiptId,
                FulfillmentResult.UNAVAILABLE
            )
        }
    }

    override fun failedOrder(order: Orderz) {
        LogUtilz.log.v(TAG, "failedOrder")
        if (order is AmazonOrder) {
            PurchasingService.notifyFulfillment(
                order.response.receipt.receiptId,
                FulfillmentResult.UNAVAILABLE
            )
        }
    }

    override fun destroy() {
        LogUtilz.log.v(TAG, "destroy")
        mainScope.cancel()
    }

    companion object {
        private const val TAG = "AmazonSales"
    }
}
