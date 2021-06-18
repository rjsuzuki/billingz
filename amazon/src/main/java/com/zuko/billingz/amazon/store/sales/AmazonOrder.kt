package com.zuko.billingz.amazon.store.sales

import com.zuko.billingz.lib.store.sales.Order

data class AmazonOrder(
    override var orderId: String?,
    override var packageName: String?,
    override var orderTime: Long?,
    override var orderToken: String?
) : Order {
}