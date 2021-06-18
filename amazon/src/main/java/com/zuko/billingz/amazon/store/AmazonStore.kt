package com.zuko.billingz.amazon.store

import android.app.Activity
import android.content.Context
import androidx.collection.ArrayMap
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LiveData
import com.amazon.device.iap.PurchasingListener
import com.amazon.device.iap.PurchasingService
import com.amazon.device.iap.model.ProductDataResponse
import com.amazon.device.iap.model.PurchaseResponse
import com.amazon.device.iap.model.PurchaseUpdatesResponse
import com.amazon.device.iap.model.UserDataResponse
import com.android.billingclient.api.Purchase
import com.zuko.billingz.amazon.store.client.AmazonClient
import com.zuko.billingz.amazon.store.inventory.AmazonInventory
import com.zuko.billingz.lib.LogUtil
import com.zuko.billingz.lib.store.Store
import com.zuko.billingz.lib.store.StoreLifecycle
import com.zuko.billingz.lib.store.agent.Agent
import com.zuko.billingz.lib.store.inventory.Inventory
import com.zuko.billingz.lib.store.products.Product
import com.zuko.billingz.lib.store.sales.GetReceiptsListener
import com.zuko.billingz.lib.store.sales.Order
import com.zuko.billingz.lib.store.sales.Sales
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel

class AmazonStore: Store {

    private val client = AmazonClient()
    private val inventory = AmazonInventory()
    // todo sales/history

    private val mainScope = MainScope()

    private var context: Context? = null

    private val agent = object: Agent {
        override fun isBillingClientReady(): LiveData<Boolean> {
            return client.isClientReady
        }

        override fun startOrder(
            activity: Activity?,
            productId: String?,
            listener: Sales.OrderValidatorListener?
        ): LiveData<Order> {
            TODO("Not yet implemented")
        }

        override fun getInventory(): Inventory {
            return inventory
        }

        override fun getPendingOrders(): ArrayMap<String, Purchase> {
            TODO("Not yet implemented")
        }

        override fun getIncompleteOrder(listener: Sales.OrderValidatorListener): LiveData<Order> {
            TODO("Not yet implemented")
        }

        override fun getReceipts(type: Product.Type, listener: GetReceiptsListener) {
            TODO("Not yet implemented")
        }

    }

    override fun getAgent(): Agent {
        return agent
    }

    override fun init(context: Context?) {
         this.context = context
    }

    override fun create() {
        client.initClient(context)
    }

    override fun start() {
        TODO("Not yet implemented")
    }

    override fun resume() {
        // Call this method to retrieve the app-specific ID and marketplace for the user who is
        // currently logged on. For example, if a user switched accounts or if multiple users
        // accessed your app on the same device, this call will help you make sure that the receipts
        // that you retrieve are for the current user account.
        val userRequestId = PurchasingService.getUserData()

        // retrieves all Subscription and Entitlement purchases across all devices. A consumable
        // purchase can be retrieved only from the device where it was purchased. getPurchaseUpdates
        // retrieves only unfulfilled and cancelled consumable purchases. Amazon recommends that you
        // persist the returned PurchaseUpdatesResponse data and query the system only for updates.
        // The response is paginated.
        val purchaseUpdatesRequestId = PurchasingService.getPurchaseUpdates(true)

        // Call this method to retrieve item data for a set of SKUs to display in your app.
        // Call getProductData in the OnResume method.
        val productDataRequestId = PurchasingService.getProductData()
    }

    override fun pause() {
        TODO("Not yet implemented")
    }

    override fun stop() {
        mainScope.cancel()
    }

    override fun destroy() {
        mainScope.cancel()
        inventory.destroy()
        client.destroy()
    }

    companion object {
        private const val TAG = "AmazonStore"
    }
}