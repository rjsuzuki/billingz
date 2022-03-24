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

/**
 * Order objects are transactions in-progress or incomplete.
 */
interface Orderz : ModuleIdentifier {
    /**
     * The current lifecycle step of the order.
     */
    var state: State

    /**
     * Status or error message.
     */
    val resultMessage: String

    /**
     * Enumerated status or error.
     */
    val result: Result

    /**
     * A unique order identifier for the transaction.
     * Amazon IAP equivalent is receiptId
     */
    val orderId: String?

    /**
     * The time the product was purchased, in milliseconds since the epoch (Jan 1, 1970)
     */
    val orderTime: Long

    /**
     * A string token that uniquely identifies a purchase for a given item and user pair
     */
    val entitlement: String?

    /**
     * Determines if the order has been cancelled.
     */
    val isCancelled: Boolean

    /**
     * Subscriptions are always 1.
     * IAPs are 1+
     */
    val quantity: Int

    /**
     * Details of the order in JSON
     */
    val originalJson: String?

    /**
     * List of all products associated with this order id.
     * Minimum size of 1 - Subscriptions are always 1.
     */
    val skus: List<String>?

    val signature: String?


    /**
     * Indicates the lifecycle of an order.
     */
    enum class State {
        UNKNOWN,
        PROCESSING,
        VALIDATING,
        COMPLETE,
        FAILED,
        CANCELED,
        PENDING
    }

    /**
     * Indicates the final state of an order.
     */
    enum class Result(val code: Int) {
        SERVICE_TIMEOUT(-3),
        NOT_SUPPORTED(-2),
        SERVICE_DISCONNECTED(-1),
        SUCCESS(0),
        USER_CANCELED(1),
        SERVICE_UNAVAILABLE(2),
        BILLING_UNAVAILABLE(3),
        INVALID_PRODUCT(4),
        DEVELOPER_ERROR(5),
        ERROR(6), // FAILED
        PRODUCT_ALREADY_OWNED(7),
        PRODUCT_NOT_OWNED(8),
        NO_RESULT(9)
    }
}
