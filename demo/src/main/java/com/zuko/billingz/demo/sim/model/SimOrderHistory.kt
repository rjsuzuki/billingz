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

import com.zuko.billingz.core.store.model.OrderHistoryz
import com.zuko.billingz.core.store.model.Receiptz
import com.zuko.billingz.demo.sim.StoreType

/**
 * In-memory [OrderHistoryz] — a snapshot of the receipts the simulated user currently owns,
 * keyed by entitlement id (purchase token for Google, receipt id for Amazon).
 */
class SimOrderHistory(
    private val storeType: StoreType,
    override val receipts: Map<String, Receiptz>
) : OrderHistoryz {

    override fun isGoogle(): Boolean = storeType == StoreType.GOOGLE

    override fun isAmazon(): Boolean = storeType == StoreType.AMAZON
}
