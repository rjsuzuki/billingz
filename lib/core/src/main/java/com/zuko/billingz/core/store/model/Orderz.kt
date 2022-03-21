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

package com.zuko.billingz.core.store.model

import com.zuko.billingz.core.misc.ModuleIdentifier

interface Orderz: ModuleIdentifier {

    /**
     * A unique order identifier for the transaction.
     * Amazon IAP equivalent is receiptId
     */
    var orderId: String?

    /**
     * The time the product was purchased, in milliseconds since the epoch (Jan 1, 1970)
     */
    var orderTime: Long

    /**
     * A string token that uniquely identifies a purchase for a given item and user pair
     */
    var entitlement: String?

    /**
     *
     */
    var state: State

    /**
     *
     */
    var isCancelled: Boolean

    /**
     * Subscriptions are always 1.
     * IAPs are 1+
     */
    var quantity: Int

    /**
     * Details of the order in JSON
     */
    var originalJson: String?

    /**
     * List of all products associated with this order id.
     * Minimum size of 1 - Subscriptions are always 1.
     */
    var skus: List<String>?

    enum class State {
        UNKNOWN,
        PROCESSING,
        VALIDATING,
        COMPLETE,
        FAILED,
        PENDING
    }
}
