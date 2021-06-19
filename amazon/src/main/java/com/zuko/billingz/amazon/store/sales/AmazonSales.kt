package com.zuko.billingz.amazon.store.sales

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.amazon.device.iap.PurchasingService
import com.zuko.billingz.lib.store.model.Product
import com.zuko.billingz.lib.store.model.Order
import com.zuko.billingz.lib.store.model.Receipt
import com.zuko.billingz.lib.store.sales.Sales

class AmazonSales: Sales {

    override var currentOrder: MutableLiveData<Order> = MutableLiveData()
    override var currentReceipt: MutableLiveData<Receipt> = MutableLiveData()
    override var orderHistory: MutableLiveData<List<Receipt>> = MutableLiveData()

    override fun startOrder(sku: String) {
        PurchasingService.purchase(sku)
    }

    override fun processOrder(order: Order) {
        validateOrder(order)
    }

    override fun validateOrder(order: Order) {
        //validator listener
        //if valid - completeOrder(order)
        //if invalid - handle gracefully
        // Verify the receipts from the purchase by having your back-end server
        // verify the receiptId with Amazon's Receipt Verification Service (RVS) before fulfilling the item
    }

    override fun completeOrder(order: Order) {


        // update history
        refreshReceipts()
    }

    override fun handleError(order: Order) {
        TODO("Not yet implemented")
    }

    override fun refreshReceipts() {
        val purchaseUpdatesRequestId = PurchasingService.getPurchaseUpdates(false)
        Log.d(TAG, "Refresh receipts: $purchaseUpdatesRequestId")
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