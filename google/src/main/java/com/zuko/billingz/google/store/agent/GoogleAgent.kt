package com.zuko.billingz.google.store.agent

import android.app.Activity
import androidx.collection.ArrayMap
import androidx.lifecycle.LiveData
import com.android.billingclient.api.Purchase
import com.zuko.billingz.lib.store.agent.Agent
import com.zuko.billingz.lib.store.inventory.Inventory
import com.zuko.billingz.lib.store.products.Product
import com.zuko.billingz.lib.store.sales.GetReceiptsListener
import com.zuko.billingz.lib.store.sales.Order
import com.zuko.billingz.lib.store.sales.Sales

class GoogleAgent: Agent {
    override fun isBillingClientReady(): LiveData<Boolean> {
        TODO("Not yet implemented")
    }

    override fun startOrder(
        activity: Activity?,
        productId: String?,
        listener: Sales.OrderValidatorListener?
    ): LiveData<Order> {
        TODO("Not yet implemented")
    }

    override fun getInventory(): Inventory {
        TODO("Not yet implemented")
    }

    override fun getPendingOrders(): ArrayMap<String, Purchase> {
        TODO("Not yet implemented")
    }

    override fun getIncompleteOrder(listener: Sales.OrderValidatorListener): LiveData<Order> {
        TODO("Not yet implemented")
    }

    override fun getReceipts(type: Product.Type, listener: GetReceiptsListener) {
        TODO("Not yet implemented")
    }
}