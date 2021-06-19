package com.zuko.billingz.amazon.store.client

import android.content.Context
import android.util.Log
import androidx.collection.ArrayMap
import androidx.lifecycle.MutableLiveData
import com.amazon.device.iap.PurchasingListener
import com.amazon.device.iap.PurchasingService
import com.amazon.device.iap.model.ProductDataResponse
import com.amazon.device.iap.model.PurchaseResponse
import com.amazon.device.iap.model.PurchaseUpdatesResponse
import com.amazon.device.iap.model.UserDataResponse

import com.zuko.billingz.amazon.store.product.AmazonProduct
import com.zuko.billingz.amazon.store.sales.AmazonOrder
import com.zuko.billingz.amazon.store.sales.AmazonReceipt
import com.zuko.billingz.lib.store.client.Client
import com.zuko.billingz.lib.store.inventory.Inventory
import com.zuko.billingz.lib.store.model.Product
import com.zuko.billingz.lib.store.model.Receipt
import com.zuko.billingz.lib.store.sales.Sales

class AmazonClient(val inventory: Inventory, val sales: Sales): Client {

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

    override fun init(context: Context?, connectionListener: Client.ConnectionListener) {

        Log.v(TAG, "initClient")
        PurchasingService.registerListener(context, object: PurchasingListener {

            override fun onUserDataResponse(response: UserDataResponse?) {
                // Invoked after a call to getUserData().
                // Determines the UserId and marketplace of the currently logged on user.
                when(response?.requestStatus) {
                    UserDataResponse.RequestStatus.SUCCESSFUL -> {
                        Log.d(TAG, "Successful user data request: ${response.requestId}")
                        userDataResponse = response
                        isConnected = true
                    }
                    UserDataResponse.RequestStatus.FAILED -> {
                        Log.e(TAG, "Failed user data request: ${response.requestId}")
                        isConnected = false
                    }
                    UserDataResponse.RequestStatus.NOT_SUPPORTED -> {
                        isConnected = false
                        Log.wtf(TAG, "Unsupported user data request: ${response.requestId}")
                    }
                }
            }

            override fun onProductDataResponse(response: ProductDataResponse?) {
                // Invoked after a call to getProductDataRequest(java.util.Set skus).
                // Retrieves information about SKUs you would like to sell from your app.
                // Use the valid SKUs in onPurchaseResponse().
                when(response?.requestStatus) {
                    ProductDataResponse.RequestStatus.SUCCESSFUL -> {
                        Log.d(TAG, "Successful product data request: ${response.requestId}")

                        // convert
                        val products = ArrayMap<String, Product>()
                        for(r in response.productData) {
                            val product = AmazonProduct(r.value)
                            products[r.key] = product
                            Log.v(TAG, "Validated product: $product")
                        }
                        inventory.allProducts = products

                        // cache
                        val unavailableSkusSet = response.unavailableSkus // todo
                    }
                    ProductDataResponse.RequestStatus.FAILED -> {
                        Log.e(TAG, "Failed product data request: ${response.requestId}")
                    }
                    ProductDataResponse.RequestStatus.NOT_SUPPORTED -> {
                        Log.wtf(TAG, "Unsupported product data request: ${response.requestId}")
                    }
                }
            }

            override fun onPurchaseResponse(response: PurchaseResponse?) {
                // Invoked after a call to purchase(String sku).
                // Used to determine the status of a purchase.
                when(response?.requestStatus) {
                    PurchaseResponse.RequestStatus.SUCCESSFUL -> {
                        Log.d(TAG, "Successful purchase request: ${response.requestId}")
                        // convert to order
                        val order = AmazonOrder(response)
                        sales.validateOrder(order)
                    }
                    PurchaseResponse.RequestStatus.FAILED -> {
                        Log.e(TAG, "Failed purchase request: ${response.requestId}")
                        val order = AmazonOrder(response)
                        sales.validateOrder(order)
                    }
                    PurchaseResponse.RequestStatus.ALREADY_PURCHASED -> {
                        Log.w(TAG, "Already purchased product for purchase request: ${response.requestId}")
                        val order = AmazonOrder(response)
                        sales.validateOrder(order)
                    }
                    PurchaseResponse.RequestStatus.INVALID_SKU -> {
                        Log.w(TAG, "Invalid sku id for purchase request: ${response.requestId}")
                        val order = AmazonOrder(response)
                        sales.validateOrder(order)
                    }
                    PurchaseResponse.RequestStatus.NOT_SUPPORTED -> {
                        Log.wtf(TAG, "Unsupported purchase request: ${response.requestId}")
                        val order = AmazonOrder(response)
                        sales.validateOrder(order)
                    }
                }
            }

            override fun onPurchaseUpdatesResponse(response: PurchaseUpdatesResponse?) {
                // Invoked after a call to getPurchaseUpdates(boolean reset).
                // Retrieves the purchase history.
                // Amazon recommends that you persist the returned PurchaseUpdatesResponse
                // data and query the system only for updates.
                when(response?.requestStatus) {
                    PurchaseUpdatesResponse.RequestStatus.SUCCESSFUL -> {
                        Log.d(TAG, "Successful purchase updates request: ${response.requestId}")

                        // convert receipts
                        val list = mutableListOf<Receipt>()
                        for(r in response.receipts) {
                            val receipt = AmazonReceipt(r)
                            receipt.userId = response.userData.userId
                            receipt.marketplace = response.userData.marketplace
                            list.add(receipt)
                        }
                        sales.orderHistory.value = list
                    }
                    PurchaseUpdatesResponse.RequestStatus.FAILED -> {
                        Log.e(TAG, "Failed purchase updates request: ${response.requestId}")
                    }
                    PurchaseUpdatesResponse.RequestStatus.NOT_SUPPORTED -> {
                        Log.wtf(TAG, "Unsupported purchase update request: ${response.requestId}")
                    }
                }
            }
        })
        isInitialized = true
    }

    override fun connect() {
        Log.v(TAG, "connect")
        // Call this method to retrieve the app-specific ID and marketplace for the user who is
        // currently logged on. For example, if a user switched accounts or if multiple users
        // accessed your app on the same device, this call will help you make sure that the receipts
        // that you retrieve are for the current user account.
        val userRequestId = PurchasingService.getUserData() // client
        Log.d(TAG, "Requesting user data: $userRequestId")
    }

    override fun disconnect() {
        Log.v(TAG, "disconnect")
    }

    override fun checkConnection() {
        Log.v(TAG, "checkConnection")
    }

    override fun destroy() {
        Log.v(TAG, "destroy")
    }

    companion object {
        private const val TAG = "AmazonClient"
    }
}