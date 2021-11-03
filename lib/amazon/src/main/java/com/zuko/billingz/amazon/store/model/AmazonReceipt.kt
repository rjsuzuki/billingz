/*
 * Copyright 2021 rjsuzuki
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.zuko.billingz.amazon.store.model

import com.amazon.device.iap.model.Receipt
import com.zuko.billingz.core.store.model.Orderz
import com.zuko.billingz.core.store.model.Receiptz
import java.util.Date

data class AmazonReceipt(
    var iapReceipt: Receipt,
    override var userId: String? = null,
    override var order: Orderz? = null
) : Receiptz {
    override var entitlement: String? = iapReceipt.receiptId

    override var orderId: String? = iapReceipt.receiptId
    override var orderDate: Date? = iapReceipt.purchaseDate
    override var skus: List<String>? = listOf(iapReceipt.sku)

    override var cancelDate: Date? = iapReceipt.cancelDate
    override var isCanceled: Boolean = iapReceipt.isCanceled

    var marketplace: String? = null
}
