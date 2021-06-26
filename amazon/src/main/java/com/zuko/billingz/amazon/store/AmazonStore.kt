package com.zuko.billingz.amazon.store

import android.app.Activity
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.zuko.billingz.amazon.store.client.AmazonClient
import com.zuko.billingz.amazon.store.inventory.AmazonInventory
import com.zuko.billingz.amazon.store.sales.AmazonSales
import com.zuko.billingz.lib.LogUtil
import com.zuko.billingz.lib.store.Store
import com.zuko.billingz.lib.store.agent.Agent
import com.zuko.billingz.lib.store.client.Client
import com.zuko.billingz.lib.store.model.Order
import com.zuko.billingz.lib.store.model.Product
import com.zuko.billingz.lib.store.model.Receipt
import com.zuko.billingz.lib.store.sales.Sales
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel

class AmazonStore: Store {

    private val mainScope = MainScope()

    private val inventory = AmazonInventory()
    private val sales = AmazonSales()
    private val client = AmazonClient(inventory, sales)

    private val connectionListener = object : Client.ConnectionListener {
        override fun connected() {
            sales.refreshQueries()
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
        client.init(context, connectionListener)
    }

    override fun create() {
        LogUtil.log.v(TAG, "creating...")
        client.connect()
    }

    override fun start() {
        LogUtil.log.v(TAG, "starting...")
    }

    override fun resume() {
        LogUtil.log.v(TAG, "resuming...")
        client.checkConnection()
        if(client.isReady()) {
            sales.refreshQueries()
        }
    }

    override fun pause() {
        LogUtil.log.v(TAG, "pausing...")
    }

    override fun stop() {
        LogUtil.log.v(TAG, "stopping...")
        mainScope.cancel()
    }

    override fun destroy() {
        LogUtil.log.v(TAG, "destroying...")
        mainScope.cancel()
        inventory.destroy()
        client.destroy()
    }

    private val agent = object: Agent {

        override fun isBillingClientReady(): LiveData<Boolean> {
            return client.isClientReady
        }

        override fun startOrder(
            activity: Activity?,
            productId: String?,
            listener: Sales.OrderValidatorListener?
        ): LiveData<Order> {

            val data = MutableLiveData<Order>()
            val product = inventory.allProducts[productId]
            product?.let {
                sales.startOrder(activity, product, client)
            }
            return data
        }

        override fun getReceipts(type: Product.Type?): LiveData<List<Receipt>> {
            return sales.orderHistory
        }

        override fun queryOrders() {
            sales.queryOrders()
        }

        override fun updateInventory(skuList: List<String>, type: Product.Type) {
            inventory.queryInventory(skuList, type)
        }

        override fun getProducts(type: Product.Type?, promo: Product.Promotion?): List<Product> {
            return inventory.getProducts(
                type = type,
                promo = promo
            )
        }

        override fun getProduct(sku: String): Product? {
            return inventory.getProduct(sku)
        }
    }

    override fun getAgent(): Agent {
        return agent
    }

    companion object {
        private const val TAG = "AmazonStore"
    }
}