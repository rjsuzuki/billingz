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
 */
package com.zuko.billingz.demo.sim.model

import com.zuko.billingz.core.store.model.Orderz
import com.zuko.billingz.demo.sim.StoreType

/**
 * In-memory [Orderz] representing a purchase in-progress. The simulation mutates [state] and
 * [result] as it drives the order through the library's purchase lifecycle.
 */
class SimOrder(
    sku: String,
    private val storeType: StoreType,
    override val orderId: String,
    override val orderTime: Long,
    override val entitlement: String?,
    override var state: Orderz.State = Orderz.State.PROCESSING,
    override var result: Orderz.Result = Orderz.Result.NO_RESULT,
    override var resultMessage: String = "",
    override val quantity: Int = 1
) : Orderz {

    override var skus: List<String>? = listOf(sku)

    override val isCancelled: Boolean
        get() = state == Orderz.State.CANCELED

    override val originalJson: String? = null

    override val signature: String? = null

    override fun isGoogle(): Boolean = storeType == StoreType.GOOGLE

    override fun isAmazon(): Boolean = storeType == StoreType.AMAZON
}
