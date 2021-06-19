package com.zuko.billingz.amazon.store.sales

import android.app.Activity
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.amazon.device.iap.PurchasingService
import com.zuko.billingz.lib.LogUtil
import com.zuko.billingz.lib.store.client.Client
import com.zuko.billingz.lib.store.model.Product
import com.zuko.billingz.lib.store.model.Order
import com.zuko.billingz.lib.store.model.Receipt
import com.zuko.billingz.lib.store.sales.Sales

class AmazonSales: Sales {

    override var currentOrder: MutableLiveData<Order> = MutableLiveData()
    override var currentReceipt: Receipt?
        get() = TODO("Not yet implemented")
        set(value) {}

    override var orderHistory: MutableLiveData<List<Receipt>> = MutableLiveData()
    override var orderUpdaterListener: Sales.OrderUpdaterListener?
        get() = TODO("Not yet implemented")
        set(value) {}
    override var orderValidatorListener: Sales.OrderValidatorListener?
        get() = TODO("Not yet implemented")
        set(value) {}

    private val validatorCallback: Sales.ValidatorCallback = object : Sales.ValidatorCallback {
        override fun onSuccess(order: Order) {
            processOrder(order)
        }

        override fun onFailure(order: Order) {
            // todo handle gracefully
        }
    }

    private val updaterCallback: Sales.UpdaterCallback = object : Sales.UpdaterCallback {
        override fun complete(order: Order) {
            completeOrder(order)
        }

        override fun cancel(order: Order) {
            TODO("Not yet implemented")
        }
    }

    override fun startOrder(activity: Activity?, product: Product, client: Client) {
        PurchasingService.purchase(product.sku)
    }

    override fun validateOrder(order: Order) {
        orderValidatorListener?.validate(order, validatorCallback) ?: LogUtil.log.e(TAG, "Null validator object. Cannot complete order.")
        //validator listener
        //if valid - completeOrder(order)
        //if invalid - handle gracefully
        // Verify the receipts from the purchase by having your back-end server
        // verify the receiptId with Amazon's Receipt Verification Service (RVS) before fulfilling the item
    }


    override fun processOrder(order: Order) {

    }

    override fun completeOrder(order: Order) {


        // update history
        refreshReceipts()
    }

    override fun refreshQueries() {
        val purchaseUpdatesRequestId = PurchasingService.getPurchaseUpdates(false)
        Log.d(TAG, "Refresh receipts: $purchaseUpdatesRequestId")
    }

    override fun queryOrders() {
        TODO("Not yet implemented")
    }


    fun refreshReceipts() {

    }

    override fun queryReceipts(type: Product.Type?) {
        // retrieves all Subscription and Entitlement purchases across all devices.
        // A consumable purchase can be retrieved only from the device where it was purchased.
        // getPurchaseUpdates
        // retrieves only unfulfilled and cancelled consumable purchases. Amazon recommends that you
        // persist the returned PurchaseUpdatesResponse data and query the system only for updates.
        // The response is paginated.
        val purchaseUpdatesRequestId = PurchasingService.getPurchaseUpdates(false) // sales
        Log.d(TAG, "Query receipts: $purchaseUpdatesRequestId")
    }

    fun queryRecentHistory() {

    }

    fun queryFullHistory() {
        val purchaseUpdatesRequestId = PurchasingService.getPurchaseUpdates(true) // sales
    }

    override fun destroy() {
        TODO("Not yet implemented")
    }

    companion object {
        private const val TAG = "AmazonSales"
    }
}