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

package com.zuko.billingz.google.store.model

import com.android.billingclient.api.Purchase
import com.zuko.billingz.core.store.model.Orderz
import com.zuko.billingz.core.store.model.Receiptz
import java.util.Date

/**
 * Google Receipts only are generated if the purchase state is complete (purchased: 1)
 * If pending, the Order object can be queried so that the user can complete the transaction.
 */
data class GoogleReceipt(
    val purchase: Purchase?,
    override var userId: String? = null,
    override var order: Orderz? = null
) : Receiptz {
    override var entitlement: String? = purchase?.purchaseToken
    override var orderId: String? = order?.orderId
    override var orderDate: Date? = order?.orderTime?.let { Date(it) }
    override var skus: List<String>? = order?.skus

    override var cancelDate: Date? = null
    override var isCanceled: Boolean =
        purchase?.purchaseState == Purchase.PurchaseState.UNSPECIFIED_STATE

    override fun isGoogle(): Boolean {
        return true
    }

    override fun isAmazon(): Boolean {
        return false
    }

    var quantity: Int = 1
    var signature: String? = purchase?.signature
    var originalJson: String? = purchase?.originalJson
}
