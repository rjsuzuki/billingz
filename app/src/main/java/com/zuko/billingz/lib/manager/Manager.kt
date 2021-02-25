package com.zuko.billingz.lib.manager

import android.app.Activity
import android.content.Context
import androidx.lifecycle.*
import com.android.billingclient.api.*
import com.zuko.billingz.lib.LogUtil
import com.zuko.billingz.lib.client.Client
import com.zuko.billingz.lib.inventory.Inventory
import com.zuko.billingz.lib.client.Billing
import com.zuko.billingz.lib.facade.BillingAgent
import com.zuko.billingz.lib.products.Consumable
import com.zuko.billingz.lib.products.NonConsumable
import com.zuko.billingz.lib.products.Product
import com.zuko.billingz.lib.products.Subscription
import com.zuko.billingz.lib.sales.*

import kotlinx.coroutines.*

/**
 * @author rjsuzuki
 */
class Manager: LifecycleObserver, ManagerLifecycle {

    private val mainScope = MainScope()

    private val billing: Billing = Client()
    private val inventory = Inventory(billing)
    private val sales: Sales = ProductSales(inventory)
    private val history: History = OrderHistory(billing)
    private var isInitialized = false


    private val googlePlayConnectListener = object: Billing.GooglePlayConnectListener {
        override fun connected() {
            history.refreshPurchaseHistory(sales)
        }
    }
    /*****************************************************************************************************
     * Lifecycle events - developer must either add this class to a lifecycleOwner or manually add the events
     * to their respective parent view
     *****************************************************************************************************/
    init {
        LogUtil.log.v(TAG, "instantiating...")
    }

    override fun init(context: Context?) {
        LogUtil.log.v(TAG, "initializing...")
        billing.initClient(context, sales.purchasesUpdatedListener, googlePlayConnectListener)
        isInitialized = true
    }

    override fun create() {
        LogUtil.log.v(TAG, "creating...")
        setOrderUpdateListener()
        if(isInitialized && billing.initialized()) {
            billing.connect()
        }
        history.refreshPurchaseHistory(sales) //might need to react to connection
    }

    override fun resume() {
        LogUtil.log.v(TAG, "resuming...")
        billing.checkConnection()
        history.refreshPurchaseHistory(sales)
    }

    override fun pause() {
        LogUtil.log.v(TAG, "pausing...")
        //todo billing.disconnect()
    }

    override fun destroy() {
        LogUtil.log.v(TAG, "destroying...")
        billing.destroy()
        history.destroy()
        sales.destroy()
        inventory.destroy()
        mainScope.cancel()
    }

    /*****************************************************************************************************
     * Private methods
     *****************************************************************************************************/
    private fun setOrderUpdateListener() {
        sales.orderUpdateListener = object: Sales.OrderUpdateListener {
            override fun resumeOrder(purchase: Purchase, productType: Product.ProductType) {
                LogUtil.log.i(TAG, "Attempting to complete purchase order : $purchase, type: $productType")
                when(productType) {
                    Product.ProductType.SUBSCRIPTION -> {
                        Subscription.completeOrder(billing.getBillingClient(), purchase, sales.order, mainScope = mainScope)
                    }
                    Product.ProductType.NON_CONSUMABLE -> {
                        NonConsumable.completeOrder(billing.getBillingClient(), purchase, sales.order, mainScope = mainScope)
                    }

                    Product.ProductType.CONSUMABLE -> {
                        Consumable.completeOrder(billing.getBillingClient(), purchase, sales.order)
                    }
                    else -> LogUtil.log.v(TAG, "Unhandled product type: $productType")
                }
            }
        }
    }

    private val billingAgent = object : BillingAgent {

        override fun purchase(
            activity: Activity?,
            productId: String?,
            listener: Sales.OrderValidatorListener?
        ): LiveData<Order> {
            LogUtil.log.v(TAG, "Starting purchase flow")
            sales.orderValidatorListener = listener

            val skuDetails = inventory.allProducts[productId]

            activity?.let { a ->
                skuDetails?.let {
                    billing.getBillingClient()?.let { client ->
                        val result = sales.startPurchaseRequest(a, skuDetails, client)
                        val order = Order(
                            billingResult = result,
                            msg = "Processing..."
                        )
                        sales.order.postValue(order)
                    }
                }
            }
            return sales.order
        }

        override fun addProductsToInventory(
            skuList: MutableList<String>,
            productType: Product.ProductType
        ) {
            LogUtil.log.v(TAG, "addProductsToInventory")
            when(productType) {
                Product.ProductType.NON_CONSUMABLE -> inventory.loadInAppProducts(skuList, false)
                Product.ProductType.CONSUMABLE -> inventory.loadInAppProducts(skuList, true)
                Product.ProductType.SUBSCRIPTION -> inventory.loadSubscriptionProducts(skuList)
                Product.ProductType.FREE_CONSUMABLE -> inventory.loadFreeProducts(skuList, productType)
                Product.ProductType.FREE_NON_CONSUMABLE -> inventory.loadFreeProducts(skuList, productType)
                Product.ProductType.FREE_SUBSCRIPTION -> inventory.loadFreeProducts(skuList, productType)
                Product.ProductType.PROMO_CONSUMABLE -> inventory.loadPromotions(skuList, productType)
                Product.ProductType.PROMO_NON_CONSUMABLE -> inventory.loadPromotions(skuList, productType)
                Product.ProductType.PROMO_SUBSCRIPTION -> inventory.loadPromotions(skuList, productType)
                else -> LogUtil.log.w(TAG, "Unhandled product type: $productType")
            }
        }

        override fun getInAppProductsHistory(): MutableList<Purchase> {
            return history.getInAppProductsHistory()
        }

        override fun getSubscriptionHistory(): MutableList<Purchase> {
            return history.getSubscriptionHistory()
        }

        //move to initializer
        override fun setOrderValidator(validator: Sales.OrderValidatorListener) {
            sales.orderValidatorListener = validator
        }

        //todo fun cancel() ???
        //todo is this necessary?
        override fun getPurchaseHistory(skuType: String, listener: PurchaseHistoryResponseListener) {
            billing.getBillingClient()?.queryPurchaseHistoryAsync(skuType, listener)
        }
    }

    /*****************************************************************************************************
     * Public Methods - Facade Pattern
     *****************************************************************************************************/
    
    fun getAgent(): BillingAgent {
        return billingAgent
    }

    companion object {
        private const val TAG = "Manager"
    }

}