/*
 *
 *  * Copyright 2021 rjsuzuki
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  *
 *
 */
package com.zuko.billingz.amazon.store.model

import com.amazon.device.iap.model.PurchaseResponse
import com.zuko.billingz.core.store.model.Orderz
import com.zuko.billingz.core.store.model.Productz

/**
 * Represents an Order.
 * @property response
 */
data class AmazonOrder(val response: PurchaseResponse) : Orderz {

    var product: Productz? = null

    override var orderId: String? = response.receipt.receiptId
    override var orderTime: Long = response.receipt.purchaseDate.time

    // UUID.randomUUID().toString()
    // create a way to determine if amazon product has been granted entitlement
    override var entitlement: String? = null
    override var skus: List<String>? = listOf(response.receipt.sku)
    override var state: Orderz.State = Orderz.State.UNKNOWN
    override var isCancelled: Boolean = response.receipt.isCanceled
    override var quantity: Int = 1
    override var originalJson: String? = response.toJSON().toString()

    override fun isAmazon(): Boolean {
        return true
    }

    override fun isGoogle(): Boolean {
        return false
    }
}
