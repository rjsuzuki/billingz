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
import com.amazon.device.iap.model.Receipt
import com.amazon.device.iap.model.RequestId
import com.amazon.device.iap.model.UserData
import com.zuko.billingz.core.store.model.Orderz
import com.zuko.billingz.core.store.model.Productz
import org.json.JSONObject

/**
 * Represents an Order.
 * The construction parameters can be passed from a [PurchaseResponse] object or from
 * a [Receipt] that has been identified as an incomplete order.
 * @property json will reflect the true origin of the data objects.
 */
data class AmazonOrder(
    val requestId: RequestId,
    val userData: UserData,
    val receipt: Receipt,
    val json: JSONObject
) : Orderz {

    var product: Productz? = null

    override var orderId: String? = receipt.receiptId
    override var orderTime: Long = receipt.purchaseDate.time
    override var entitlement: String? = null
    override var skus: List<String>? = listOf(receipt.sku)
    override var state: Orderz.State = Orderz.State.UNKNOWN
    override var isCancelled: Boolean = receipt.isCanceled
    override var quantity: Int = 1
    override var originalJson: String? = json.toString()

    override fun isAmazon(): Boolean {
        return true
    }

    override fun isGoogle(): Boolean {
        return false
    }
}
