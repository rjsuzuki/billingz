package com.zuko.billingz.amazon.store.inventory

import com.amazon.device.iap.model.ProductDataResponse
import com.zuko.billingz.core.store.inventory.Inventoryz

sealed interface AmazonInventoryz : Inventoryz {
    fun handleQueriedProducts(response: ProductDataResponse?)
}
