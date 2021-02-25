package com.zuko.billingz.lib.manager

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.annotation.UiThread
import androidx.lifecycle.*
import com.android.billingclient.api.*
import com.zuko.billingz.lib.LogUtil
import com.zuko.billingz.lib.client.Client
import com.zuko.billingz.lib.inventory.Inventory
import com.zuko.billingz.lib.client.Billing
import com.zuko.billingz.lib.products.Consumables
import com.zuko.billingz.lib.products.Product
import com.zuko.billingz.lib.sales.*

import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

/**
 * @author rjsuzuki
 */
class Manager(val context: Context): LifecycleObserver, CoroutineScope {

    override val coroutineContext: CoroutineContext = SupervisorJob() + Dispatchers.Main

    private val billing: Billing = Client(context)
    private val inventory = Inventory(billing)
    private val history: History = OrderHistory(billing, inventory)
    private val sales: Sales = ProductSales(inventory)

    /*****************************************************************************************************
     * Lifecycle events - developer must either add this class to a lifecycleOwner or manually add the events
     * to their respective parent view
     *****************************************************************************************************/

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun create() {
        billing.initClient(context, sales.purchasesUpdatedListener)
        billing.connect()
        history.refreshPurchaseHistory(true)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun resume() {
        history.refreshPurchaseHistory(false)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun pause() {
        //todo billing.disconnect()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun destroy() {
        billing.disconnect()
        cancel()
    }

    /*****************************************************************************************************
     * Private methods
     *****************************************************************************************************/

    private val orderUpdateListener = object: Sales.OrderUpdateListener {

        override fun completeOrder(purchase: Purchase, productType: Product.ProductType) {
            LogUtil.log.i(TAG, "Attempting to complete purchase order : $purchase, type: $productType")
            when(productType) {
                Product.ProductType.SUBSCRIPTION -> {
                    val listener = AcknowledgePurchaseResponseListener { billingResult ->
                        val data = Order(
                            purchase = purchase,
                            billingResult = billingResult,
                            msg = "Subscription successfully purchased"
                        )
                        billing.order.postValue(data)
                    }

                    if(purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        if (!purchase.isAcknowledged) {
                            val acknowledgePurchaseParams =
                                AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken)

                            launch(Dispatchers.IO) {
                                billing.getBillingClient()
                                    ?.acknowledgePurchase(acknowledgePurchaseParams.build(), listener)
                            }
                        }
                    }
                }
                Product.ProductType.NON_CONSUMABLE -> {
                    val listener = AcknowledgePurchaseResponseListener { billingResult ->
                        val data = Order(
                            purchase = purchase,
                            billingResult = billingResult,
                            msg = "Non-Consumable successfully purchased"
                        )
                        billing.order.postValue(data)
                    }

                    if(purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        if(!purchase.isAcknowledged) {
                            val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken)
                            launch(Dispatchers.IO) {
                                billing.getBillingClient()?.acknowledgePurchase(acknowledgePurchaseParams.build(), listener)
                            }
                        }
                    }
                }
                Product.ProductType.CONSUMABLE -> {

                    val consumeParams = ConsumeParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()

                    billing.getBillingClient()?.consumeAsync(consumeParams) { billingResult, _ ->
                        val msg: String
                        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            msg = "Consumable successfully purchased"
                            Log.d(TAG, "Product successfully purchased: ${purchase.sku}")
                        } else {
                            msg = billingResult.debugMessage
                            billing.error(billingResult)
                        }
                        val data = Order(
                            purchase = purchase,
                            billingResult = billingResult,
                            msg = msg
                        )
                        billing.order.postValue(data)
                    }
                }
                else -> LogUtil.log.v(TAG, "Unhandled product type: $productType")
            }
        }
    }
    /*****************************************************************************************************
     * Public Methods - Facade Pattern
     *****************************************************************************************************/



    @UiThread
    fun purchase(activity: Activity?, productId: String?, listener: Sales.OrderValidatorListener?) : LiveData<Order> {
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
                    sales.currentOrder.postValue(order)
                }
            }
        }
        return sales.currentOrder
    }

    /**
     * @param skuList: MutableList<String>
     * @param productType: Product.ProductType
     * @param skuType: BillingClient.SkuType
     */
    fun addProductsToInventory(skuList: MutableList<String>,
                               productType: Product.ProductType) {
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

    fun getInAppProductsHistory(): MutableList<Purchase> {
        return history.getInAppProductsHistory()
    }

    fun getSubscriptionHistory(): MutableList<Purchase> {
        return history.getSubscriptionHistory()
    }

    companion object {
        private const val TAG = "Manager"
    }

}