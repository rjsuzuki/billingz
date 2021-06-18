package com.zuko.billingz.amazon.store.client

import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.amazon.device.iap.PurchasingListener
import com.amazon.device.iap.PurchasingService
import com.amazon.device.iap.model.ProductDataResponse
import com.amazon.device.iap.model.PurchaseResponse
import com.amazon.device.iap.model.PurchaseUpdatesResponse
import com.amazon.device.iap.model.UserDataResponse
import com.android.billingclient.api.PurchasesUpdatedListener
import com.zuko.billingz.lib.store.client.Client

class AmazonClient: Client {

    override var isClientReady = MutableLiveData<Boolean>()
    private var isInitialized = false
    private var isConnected = false

    override fun initialized(): Boolean {
        return isInitialized
    }

    override fun isReady(): Boolean {
        TODO("Not yet implemented")
    }

    override fun initClient(
        context: Context?,
        purchasesUpdatedListener: PurchasesUpdatedListener,
        googlePlayConnectListener: Client.ConnectionListener
    ) {
        PurchasingService.registerListener(context, object: PurchasingListener {

            override fun onUserDataResponse(response: UserDataResponse?) {
                // Invoked after a call to getUserData().
                // Determines the UserId and marketplace of the currently logged on user.
                when(response?.requestStatus) {
                    UserDataResponse.RequestStatus.SUCCESSFUL -> {
                        val currentId = response.userData?.userId
                        val currentMarketplace = response.userData?.marketplace
                    }
                    UserDataResponse.RequestStatus.FAILED -> {

                    }
                    UserDataResponse.RequestStatus.NOT_SUPPORTED -> {

                    }
                }
            }

            override fun onProductDataResponse(response: ProductDataResponse?) {
                // Invoked after a call to getProductDataRequest(java.util.Set skus).
                //  Retrieves information about SKUs you would like to sell from your app. Use the valid SKUs in onPurchaseResponse().
                when(response?.requestStatus) {
                    ProductDataResponse.RequestStatus.SUCCESSFUL -> {}
                    ProductDataResponse.RequestStatus.FAILED -> {}
                    ProductDataResponse.RequestStatus.NOT_SUPPORTED -> {}
                }
            }

            override fun onPurchaseResponse(response: PurchaseResponse?) {
                // Invoked after a call to purchase(String sku). Used to determine the status of a purchase.
                when(response?.requestStatus) {
                    PurchaseResponse.RequestStatus.SUCCESSFUL -> {}
                    PurchaseResponse.RequestStatus.FAILED -> {}
                    PurchaseResponse.RequestStatus.ALREADY_PURCHASED -> {}
                    PurchaseResponse.RequestStatus.INVALID_SKU -> {}
                    PurchaseResponse.RequestStatus.NOT_SUPPORTED -> {}
                }
            }

            override fun onPurchaseUpdatesResponse(response: PurchaseUpdatesResponse?) {
                // Invoked after a call to getPurchaseUpdates(boolean reset). Retrieves the purchase history.
                // Amazon recommends that you persist the returned PurchaseUpdatesResponse data and query the system only for updates.
                when(response?.requestStatus) {
                    PurchaseUpdatesResponse.RequestStatus.SUCCESSFUL -> {}
                    PurchaseUpdatesResponse.RequestStatus.FAILED -> { }
                    PurchaseUpdatesResponse.RequestStatus.NOT_SUPPORTED -> {}
                }
            }
        })
    }

    override fun connect() {
        TODO("Not yet implemented")
    }

    override fun disconnect() {
        TODO("Not yet implemented")
    }

    override fun checkConnection() {
        TODO("Not yet implemented")
    }

    override fun destroy() {
        TODO("Not yet implemented")
    }
}