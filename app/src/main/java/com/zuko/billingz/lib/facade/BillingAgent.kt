package com.zuko.billingz.lib.facade

import android.app.Activity
import androidx.annotation.UiThread
import androidx.lifecycle.LiveData
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchaseHistoryResponseListener
import com.android.billingclient.api.SkuDetails
import com.zuko.billingz.lib.products.Product
import com.zuko.billingz.lib.sales.Order
import com.zuko.billingz.lib.sales.Sales

/**
 * @author rjsuzuki
 */
interface BillingAgent {

    fun isBillingClientReady(): LiveData<Boolean>

    @UiThread
    fun purchase(activity: Activity?, productId: String?, listener: Sales.OrderValidatorListener?) : LiveData<Order>

    /**
     * Handle purchases still remaining from recent history
     */
    fun queriedOrders(listener: Sales.OrderValidatorListener) : LiveData<Order>

    /**
     * @param skuList: MutableList<String>
     * @param productType: Product.ProductType
     * @param skuType: BillingClient.SkuType
     */
    fun getAvailableProducts(skuList: MutableList<String>,
                             productType: Product.ProductType): LiveData<Map<String, SkuDetails>>

    fun getProductDetails(productId: String) : SkuDetails?

    /**
     * Returns the most recent purchase made
     * by the user for each SKU, even if that purchase is expired, canceled, or consumed.
     */
    fun getPurchaseHistory(skuType: String, listener: PurchaseHistoryResponseListener)
}