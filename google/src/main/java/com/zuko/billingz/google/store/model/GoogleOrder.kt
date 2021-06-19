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
package com.zuko.billingz.google.store.model

import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.zuko.billingz.lib.store.model.Order

/**
 * Represents an Order.
 * @property billingResult
 * @property purchase
 * @property msg - shorthand for message.
 */
data class GoogleOrder(val purchase: Purchase? = null,
                       val billingResult: BillingResult?,
                       val msg: String): Order {

    override var orderId: String?
        get() = TODO("Not yet implemented")
        set(value) {}
    override var packageName: String?
        get() = TODO("Not yet implemented")
        set(value) {}
    override var orderTime: Long
        get() = TODO("Not yet implemented")
        set(value) {}
    override var orderToken: String?
        get() = TODO("Not yet implemented")
        set(value) {}
    override var status: Order.Status
        get() = TODO("Not yet implemented")
        set(value) {}

}


