package com.zuko.billingz.amazon.store

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.amazon.device.iap.PurchasingService
import com.amazon.device.iap.model.PurchaseResponse
import com.amazon.device.iap.model.Receipt
import com.zuko.billingz.amazon.store.client.AmazonClient
import com.zuko.billingz.amazon.store.inventory.AmazonInventory
import com.zuko.billingz.amazon.store.model.AmazonProduct
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

class AmazonStore private constructor(): Storez {

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
            LogUtilz.log.v(TAG, "isBillingClientReady")
            return client.isClientReady
        }

        override fun startOrder(
            activity: Activity?,
            productId: String?,
            options: Bundle?,
            listener: Salez.OrderValidatorListener?
        ): LiveData<Orderz> {
            LogUtilz.log.v(TAG, "Starting order: $productId")
            val data = MutableLiveData<Orderz>()
            val product = inventory.getProduct(productId)
            product?.let {
                sales.startOrder(activity, product, client)
            }
            return data
        }

        override fun queryOrders(): LiveData<Orderz> {
            LogUtilz.log.v(TAG, "queryOrders")
            return sales.queryOrders()
        }

        override fun queryReceipts(type: Productz.Type?): LiveData<List<Receiptz>> {
            LogUtilz.log.v(TAG, "getReceipts: $type")
            return sales.orderHistory
        }

        override fun updateInventory(products: Map<String, Productz.Type>) {
            LogUtilz.log.v(TAG, "updateInventory: ${products.size}")
            inventory.queryInventory(products = products)
        }

        override fun getProducts(type: Productz.Type?,
                                 promo: Productz.Promotion?): Map<String, Productz> {
            LogUtilz.log.v(TAG, "getProducts: $type : $promo")
            return inventory.getProducts(
                type = type,
                promo = promo
            )
        }

        override fun getProduct(sku: String?): Productz? {
            LogUtilz.log.v(TAG, "getProduct: $sku")
            return inventory.getProduct(sku = sku)
        }
    }

    override fun getAgent(): Agentz {
        return agent
    }

    /**
     * Builder Pattern - create an instance of AmazonStore
     */
    class Builder(var context: Context?) {
        private lateinit var instance: AmazonStore
        private lateinit var updaterListener: Salez.OrderUpdaterListener
        private lateinit var validatorListener: Salez.OrderValidatorListener

        /**
         * @param listener - Required to be set for proper functionality
         */
        fun setOrderUpdater(listener: Salez.OrderUpdaterListener): Builder {
            updaterListener = listener
            return this
        }

        /**
         * @param listener - Required to be set for proper functionality
         */
        fun setOrderValidator(listener: Salez.OrderValidatorListener): Builder {
            validatorListener = listener
            return this
        }

        fun build(): AmazonStore {
            if(!::instance.isInitialized) {
                instance = AmazonStore()
            }
            instance.sales.apply {
                orderUpdaterListener = updaterListener
                orderValidatorListener = validatorListener
            }
            instance.init(context = context)
            return instance
        }
    }

    companion object {
        private const val TAG = "AmazonStore"
    }
}