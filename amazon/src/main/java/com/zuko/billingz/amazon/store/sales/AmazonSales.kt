package com.zuko.billingz.amazon.store.sales

import android.app.Activity
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.amazon.device.iap.PurchasingService
import com.amazon.device.iap.model.FulfillmentResult
import com.zuko.billingz.lib.LogUtil
import com.zuko.billingz.lib.store.client.Client
import com.zuko.billingz.lib.store.model.Product
import com.zuko.billingz.lib.store.model.Order
import com.zuko.billingz.lib.store.model.Receipt
import com.zuko.billingz.lib.store.sales.Sales

//https://developer.amazon.com/docs/in-app-purchasing/iap-implement-iap.html#responsereceiver
class AmazonSales: Sales {

    override var currentOrder: MutableLiveData<Order> = MutableLiveData()
    override var currentReceipt: Receipt?
        get() = TODO("Not yet implemented")
        set(value) {}

    override var orderHistory: MutableLiveData<List<Receipt>> = MutableLiveData()
    override var orderUpdaterListener: Sales.OrderUpdaterListener? = null
    override var orderValidatorListener: Sales.OrderValidatorListener? = null

    private val validatorCallback: Sales.ValidatorCallback = object : Sales.ValidatorCallback {
        override fun onSuccess(order: Order) {
            processOrder(order)
        }

        override fun onFailure(order: Order) {
            // todo handle gracefully
            Log.wtf(TAG, "onFailure")
        }
    }

    private val updaterCallback: Sales.UpdaterCallback = object : Sales.UpdaterCallback {

        override fun complete(order: Order) {
            completeOrder(order)
        }

        override fun cancel(order: Order) {
            cancelOrder(order)
        }
    }

    // step 1
    override fun startOrder(activity: Activity?, product: Product, client: Client) {
        PurchasingService.purchase(product.sku)
    }

    // step 2
    override fun validateOrder(order: Order) {
        // Verify the receipts from the purchase by having your back-end server
        // verify the receiptId with Amazon's Receipt Verification Service (RVS) before fulfilling the item
        orderValidatorListener?.validate(order, validatorCallback) ?: LogUtil.log.e(TAG, "Null validator object. Cannot complete order.")
    }

    override fun processOrder(order: Order) {
        orderUpdaterListener?.onResume(order, updaterCallback)
    }

    override fun completeOrder(order: Order) {
        try {
            if(order is AmazonOrder) {
                if(order.response?.receipt?.isCanceled == true) {
                    // revoke
                    cancelOrder(order)
                    Log.wtf(TAG, "isCanceled")
                    return
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

    override fun queryOrders() {
        val purchaseUpdatesRequestId = PurchasingService.getPurchaseUpdates(true)
        Log.d(TAG, "Refresh receipts: $purchaseUpdatesRequestId")
        //todo - if order is pending still
        // orderUpdaterListener?.onResume(order, updaterCallback)

        // retrieves all Subscription and Entitlement purchases across all devices.
        // A consumable purchase can be retrieved only from the device where it was purchased.
        // getPurchaseUpdates
        // retrieves only unfulfilled and cancelled consumable purchases. Amazon recommends that you
        // persist the returned PurchaseUpdatesResponse data and query the system only for updates.
        // The response is paginated.
         Log.d(TAG, "Query receipts: $purchaseUpdatesRequestId")
    }

    override fun queryReceipts(type: Product.Type?) {
        val purchaseUpdatesRequestId = PurchasingService.getPurchaseUpdates(true) // sales
    }

    fun cancelOrder(order: Order) {
        if(order is AmazonOrder) {
            PurchasingService.notifyFulfillment(order.response.receipt.receiptId, FulfillmentResult.UNAVAILABLE)
        }
    }

    override fun destroy() {
        TODO("Not yet implemented")
    }

    companion object {
        private const val TAG = "AmazonSales"
    }
}