package com.zuko.billingz.amazon.store

import android.app.Activity
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.zuko.billingz.amazon.store.client.AmazonClient
import com.zuko.billingz.amazon.store.inventory.AmazonInventory
import com.zuko.billingz.amazon.store.sales.AmazonSales
import com.zuko.billingz.core.LogUtilz
import com.zuko.billingz.core.store.Storez
import com.zuko.billingz.core.store.agent.Agentz
import com.zuko.billingz.core.store.client.Clientz
import com.zuko.billingz.core.store.model.Orderz
import com.zuko.billingz.core.store.model.Productz
import com.zuko.billingz.core.store.model.Receiptz
import com.zuko.billingz.core.store.sales.Salez
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel

class AmazonStore: Storez {

    private val mainScope = MainScope()

    private val inventory = AmazonInventory()
    private val sales = AmazonSales()
    private val client = AmazonClient(inventory, sales)

    private val connectionListener = object : Clientz.ConnectionListener {
        override fun connected() {
            sales.refreshQueries()
        }
    }

    /*****************************************************************************************************
     * Lifecycle events - developer must either add this class to a lifecycleOwner or manually add the events
     * to their respective parent view
     *****************************************************************************************************/

    init {
        LogUtilz.log.v(TAG, "instantiating...")
    }

    override fun init(context: Context?) {
        LogUtilz.log.v(TAG, "initializing...")
        client.init(context, connectionListener)
    }

    override fun create() {
        LogUtilz.log.v(TAG, "creating...")
        client.connect()
    }

    override fun start() {
        LogUtilz.log.v(TAG, "starting...")
    }

    override fun resume() {
        LogUtilz.log.v(TAG, "resuming...")
        client.checkConnection()
        if(client.isReady()) {
            sales.refreshQueries()
        }
    }

    override fun pause() {
        LogUtilz.log.v(TAG, "pausing...")
    }

    override fun stop() {
        LogUtilz.log.v(TAG, "stopping...")
        mainScope.cancel()
    }

    override fun destroy() {
        LogUtilz.log.v(TAG, "destroying...")
        mainScope.cancel()
        inventory.destroy()
        client.destroy()
    }

    private val agent = object: Agentz {

        override fun isBillingClientReady(): LiveData<Boolean> {
            return client.isClientReady
        }

        override fun startOrder(
            activity: Activity?,
            productId: String?,
            listener: Salez.OrderValidatorListener?
        ): LiveData<Orderz> {

            val data = MutableLiveData<Orderz>()
            val product = inventory.allProducts[productId]
            product?.let {
                sales.startOrder(activity, product, client)
            }
            return data
        }

        override fun getReceipts(type: Productz.Type?): LiveData<List<Receiptz>> {
            return sales.orderHistory
        }

        override fun queryOrders() {
            sales.queryOrders()
        }

        override fun updateInventory(skuList: List<String>, type: Productz.Type) {
            inventory.queryInventory(skuList, type)
        }

        override fun getProducts(type: Productz.Type?, promo: Productz.Promotion?): List<Productz> {
            return inventory.getProducts(
                type = type,
                promo = promo
            )
        }

        override fun getProduct(sku: String): Productz? {
            return inventory.getProduct(sku)
        }
    }

    override fun getAgent(): Agentz {
        return agent
    }

    companion object {
        private const val TAG = "AmazonStore"
    }
}