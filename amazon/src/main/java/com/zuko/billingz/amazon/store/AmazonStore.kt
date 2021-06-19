package com.zuko.billingz.amazon.store

import android.app.Activity
import android.content.Context
import androidx.collection.ArrayMap
import androidx.lifecycle.LiveData
import com.zuko.billingz.amazon.store.client.AmazonClient
import com.zuko.billingz.amazon.store.inventory.AmazonInventory
import com.zuko.billingz.amazon.store.sales.AmazonSales
import com.zuko.billingz.lib.LogUtil
import com.zuko.billingz.lib.store.Store
import com.zuko.billingz.lib.store.agent.Agent
import com.zuko.billingz.lib.store.client.Client
import com.zuko.billingz.lib.store.model.Product
import com.zuko.billingz.lib.store.model.Order
import com.zuko.billingz.lib.store.model.Receipt
import com.zuko.billingz.lib.store.sales.Sales
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel

class AmazonStore private constructor(): Store {

    private val mainScope = MainScope()

    private val inventory = AmazonInventory()
    private val sales = AmazonSales()
    private val client = AmazonClient(inventory, sales)

    private val connectionListener = object : Client.ConnectionListener {
        override fun connected() {
            //todo sales.refreshReceipts()
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
            //todo - inventory.queryInventory(null, Product.Type.NON_CONSUMABLE)
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
            TODO("Not yet implemented")
        }

        override fun getReceipts(type: Product.Type?): LiveData<List<Receipt>> {
            TODO("Not yet implemented")
        }

        override fun queryOrders() {
            TODO("Not yet implemented")
        }

        override fun updateInventory(skuList: List<String>, type: Product.Type) {
            TODO("Not yet implemented")
        }

        override fun getProducts(type: Product.Type?, promo: Product.Promotion?): List<Product> {
            TODO("Not yet implemented")
        }

        override fun getProduct(sku: String): Product? {
            TODO("Not yet implemented")
        }
    }

    override fun getAgent(): Agent {
        return agent
    }

    companion object {
        private const val TAG = "AmazonStore"
    }

    class Builder {

        private val store = AmazonStore()

        fun create(context: Context?): Builder {
            store.init(context)
            return this
        }

        fun setConsumables(skuList: List<String>): Builder {
            store.inventory.consumableSkus.clear()
            store.inventory.consumableSkus.addAll(skuList)
            return this
        }

        fun setNonConsumables(skuList: List<String>): Builder {
            store.inventory.nonConsumableSkus.clear()
            store.inventory.nonConsumableSkus.addAll(skuList)
            return this
        }

        fun setSubscriptions(skuList: List<String>): Builder {
            store.inventory.subscriptionSkus.clear()
            store.inventory.subscriptionSkus.addAll(skuList)
            return this
        }

        fun setOrderUpdateListener(listener: Sales.OrderUpdaterListener): Builder {
            store.sales.orderUpdaterListener = listener
            return this
        }

        fun setOrderResumeListener(listener: Sales.OrderValidatorListener): Builder {
            store.sales.orderValidatorListener = listener
            return this
        }

        fun build(): AmazonStore {
            return store
        }
    }
}