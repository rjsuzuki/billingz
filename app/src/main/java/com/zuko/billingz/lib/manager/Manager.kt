package com.zuko.billingz.lib.manager

import android.app.Activity
import android.content.Context
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LiveData
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchaseHistoryResponseListener
import com.android.billingclient.api.SkuDetails
import com.zuko.billingz.lib.LogUtil
import com.zuko.billingz.lib.agent.BillingAgent
import com.zuko.billingz.lib.client.Billing
import com.zuko.billingz.lib.client.Client
import com.zuko.billingz.lib.inventory.Inventory
import com.zuko.billingz.lib.inventory.StoreInventory
import com.zuko.billingz.lib.products.Consumable
import com.zuko.billingz.lib.products.NonConsumable
import com.zuko.billingz.lib.products.Product
import com.zuko.billingz.lib.products.Subscription
import com.zuko.billingz.lib.sales.*
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel

/**
 * @author rjsuzuki
 * //TODO handle pending purchases?
 * //TODO retry connection
 */
class Manager: LifecycleObserver, ManagerLifecycle {

    private val mainScope = MainScope()

    private val billing: Billing = Client()
    private val inventory: Inventory = StoreInventory(billing)
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

    override fun start() {
        LogUtil.log.v(TAG, "starting...")
    }

    override fun resume() {
        LogUtil.log.v(TAG, "resuming...")
        billing.checkConnection()
        history.refreshPurchaseHistory(sales)
    }

    override fun pause() {
        LogUtil.log.v(TAG, "pausing...")
    }

    override fun stop() {
        LogUtil.log.v(TAG, "stopping...")
        billing.disconnect()
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
                        Subscription.completeOrder(billing.getBillingClient(), purchase, sales.getOrderOrQueried(), mainScope = mainScope)
                    }

                    Product.ProductType.NON_CONSUMABLE -> {
                        NonConsumable.completeOrder(billing.getBillingClient(), purchase, sales.getOrderOrQueried(), mainScope = mainScope)
                    }

                    Product.ProductType.CONSUMABLE -> {
                        Consumable.completeOrder(billing.getBillingClient(), purchase, sales.getOrderOrQueried(), null)
                    }
                    else -> LogUtil.log.v(TAG, "Unhandled product type: $productType")
                }
            }
        }
    }

    private val billingAgent = object : BillingAgent {

        override fun isBillingClientReady(): LiveData<Boolean> {
            return billing.isBillingClientReady
        }

        override fun queriedOrders(listener: Sales.OrderValidatorListener) : LiveData<Order> {
            sales.orderValidatorListener = listener
            return sales.queriedOrder
        }

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

        override fun getAvailableProducts(
            skuList: MutableList<String>,
            productType: Product.ProductType
        ) : LiveData<Map<String, SkuDetails>> {
            LogUtil.log.v(TAG, "addProductsToInventory")
            when(productType) {
                Product.ProductType.NON_CONSUMABLE -> inventory.loadInAppProducts(skuList, false)
                Product.ProductType.CONSUMABLE -> inventory.loadInAppProducts(skuList, true)
                Product.ProductType.SUBSCRIPTION -> inventory.loadSubscriptions(skuList)
                Product.ProductType.FREE_CONSUMABLE -> inventory.loadFreeProducts(skuList, productType)
                Product.ProductType.FREE_NON_CONSUMABLE -> inventory.loadFreeProducts(skuList, productType)
                Product.ProductType.FREE_SUBSCRIPTION -> inventory.loadFreeProducts(skuList, productType)
                Product.ProductType.PROMO_CONSUMABLE -> inventory.loadPromotions(skuList, productType)
                Product.ProductType.PROMO_NON_CONSUMABLE -> inventory.loadPromotions(skuList, productType)
                Product.ProductType.PROMO_SUBSCRIPTION -> inventory.loadPromotions(skuList, productType)
                else -> LogUtil.log.w(TAG, "Unhandled product type: $productType")
            }
            return inventory.requestedProducts
        }

        override fun getProductDetails(productId: String) : SkuDetails? {
            return inventory.getProductDetails(productId)
        }

        override fun getBillingHistory(skuType: String, listener: PurchaseHistoryResponseListener) {
            billing.getBillingClient()?.queryPurchaseHistoryAsync(skuType, listener)
        }
    }

    /*****************************************************************************************************
     * Public Methods - Facade Pattern
     *****************************************************************************************************/

    /**
     * Returns the primary class for developers to conveniently
     * interact with Android's Billing Library (Facade pattern).
     * @return [BillingAgent]
     */
    @Suppress("unused")
    fun getAgent(): BillingAgent {
        return billingAgent
    }

    companion object {
        private const val TAG = "Manager"
    }

}