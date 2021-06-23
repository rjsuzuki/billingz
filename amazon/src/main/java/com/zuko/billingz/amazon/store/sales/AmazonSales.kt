package com.zuko.billingz.amazon.store.sales

import android.app.Activity
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.amazon.device.iap.PurchasingService
import com.amazon.device.iap.model.FulfillmentResult
import com.amazon.device.iap.model.ProductType
import com.amazon.device.iap.model.PurchaseResponse
import com.zuko.billingz.amazon.store.model.AmazonOrder
import com.zuko.billingz.amazon.store.model.AmazonReceipt
import com.zuko.billingz.lib.LogUtil
import com.zuko.billingz.lib.store.client.Client
import com.zuko.billingz.lib.store.model.Product
import com.zuko.billingz.lib.store.model.Order
import com.zuko.billingz.lib.store.model.Receipt
import com.zuko.billingz.lib.store.sales.Sales
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel

//https://developer.amazon.com/docs/in-app-purchasing/iap-implement-iap.html#responsereceiver
class AmazonSales: Sales {

    private val mainScope = MainScope()

    override var currentOrder: MutableLiveData<Order> = MutableLiveData()
    override var currentReceipt = MutableLiveData<Receipt>()

    override var orderHistory: MutableLiveData<List<Receipt>> = MutableLiveData()
    override var orderUpdaterListener: Sales.OrderUpdaterListener? = null
    override var orderValidatorListener: Sales.OrderValidatorListener? = null

    private val validatorCallback: Sales.ValidatorCallback = object : Sales.ValidatorCallback {
        override fun validated(order: Order) {
            // verify amazon receipt
            processOrder(order)
        }

        override fun invalidate(order: Order) {
            Log.d(TAG, "onFailure")
            cancelOrder(order)
        }
    }

    private val updaterCallback: Sales.UpdaterCallback = object : Sales.UpdaterCallback {

        override fun complete(order: Order) {
            // fulfill order
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
        try {
            if(order is AmazonOrder) {
                if(order.response.receipt?.isCanceled == true) {
                    // revoke
                    cancelOrder(order)
                    Log.wtf(TAG, "isCanceled")
                    return
                }
                // Verify the receipts from the purchase by having your back-end server
                // verify the receiptId with Amazon's Receipt Verification Service (RVS) before fulfilling the item
                orderValidatorListener?.validate(order, validatorCallback) ?: LogUtil.log.e(TAG, "Null validator object. Cannot complete order.")
            }
        } catch (e: Exception) {
            Log.e(TAG, e.localizedMessage ?: "error")
        }
    }

    // step 3
    override fun processOrder(order: Order) {
        orderUpdaterListener?.onResume(order, updaterCallback)
    }

    // step 4
    override fun completeOrder(order: Order) {
        try {
            if(order is AmazonOrder) {

                // we check if the order is canceled again before completing
                if(order.response.receipt?.isCanceled == true) {
                    // revoke
                    cancelOrder(order)
                    Log.wtf(TAG, "isCanceled")
                    return
                }

                when(order.product?.type) {
                    Product.Type.CONSUMABLE -> completeConsumable(order.response)
                    Product.Type.NON_CONSUMABLE -> completeNonConsumable(order.response)
                    Product.Type.SUBSCRIPTION -> completeSubscription(order.response)
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

    private fun completeConsumable(response: PurchaseResponse) {
        LogUtil.log.v(TAG, "completeConsumable")
        // convert receipt to AmazonReceipt
        val amazonReceipt = AmazonReceipt(response.receipt)
        currentReceipt.postValue(amazonReceipt)
        orderUpdaterListener?.onComplete(amazonReceipt)
    }

    private fun completeNonConsumable(response: PurchaseResponse) {
        LogUtil.log.v(TAG, "completeNonConsumable")
        val amazonReceipt = AmazonReceipt(response.receipt)
        currentReceipt.postValue(amazonReceipt)
        orderUpdaterListener?.onComplete(amazonReceipt)
    }

    private fun completeSubscription(response: PurchaseResponse) {
        LogUtil.log.v(TAG, "completeSubscription")
        val amazonReceipt = AmazonReceipt(response.receipt)
        currentReceipt.postValue(amazonReceipt)
        orderUpdaterListener?.onComplete(amazonReceipt)
    }

    override fun cancelOrder(order: Order) {
        LogUtil.log.v(TAG, "cancelOrder")
        if(order is AmazonOrder) {
            PurchasingService.notifyFulfillment(order.response.receipt.receiptId, FulfillmentResult.UNAVAILABLE)
        }
    }

    override fun failedOrder(order: Order) {
        LogUtil.log.v(TAG, "failedOrder")
        if(order is AmazonOrder) {
            PurchasingService.notifyFulfillment(order.response.receipt.receiptId, FulfillmentResult.UNAVAILABLE)
        }
    }

    override fun destroy() {
        LogUtil.log.v(TAG, "destroy")
        mainScope.cancel()
    }

    companion object {
        private const val TAG = "AmazonSales"
    }
}