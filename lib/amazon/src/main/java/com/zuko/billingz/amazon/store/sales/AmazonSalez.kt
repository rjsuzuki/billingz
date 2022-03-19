package com.zuko.billingz.amazon.store.sales

import com.amazon.device.iap.model.PurchaseResponse
import com.amazon.device.iap.model.PurchaseUpdatesResponse
import com.zuko.billingz.core.store.sales.Salez

sealed interface AmazonSalez : Salez {
    fun handlePurchasedOrder(response: PurchaseResponse?)
    fun handleQueriedOrders(response: PurchaseUpdatesResponse?)
}
