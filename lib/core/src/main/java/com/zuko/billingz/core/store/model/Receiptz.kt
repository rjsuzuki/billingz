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
import java.util.Date

/**
 * Receipt objects are completed transactions with a final state as
 * canceled, expired, or consumed.
 */
interface Receiptz : ModuleIdentifier {

    /**
     * Unique id for granting entitlement of a product.
     * Google uses PurchaseTokens.
     */
    var entitlement: String?

    /**
     * Unique id of the order
     */
    var orderId: String?

    /**
     * Unique id of the user as identified by the relevant billing service.
     * Amazon only has a userData object, but Google will either provide an accountId
     * and/or profileId
     */
    var userId: String?

    /**
     * Product id
     */
    var skus: List<String>?

    /**
     * Date of purchase
     */
    var orderDate: Date?

    /**
     * Date of cancellation,
     * null if not available.
     */
    var cancelDate: Date?

    /**
     *
     */
    var isCanceled: Boolean

    var order: Orderz?
}
