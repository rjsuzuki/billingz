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

package com.zuko.billingz.amazon.store.sales

import com.amazon.device.iap.model.PurchaseResponse
import com.amazon.device.iap.model.PurchaseUpdatesResponse
import com.zuko.billingz.core.store.sales.Salez

interface AmazonSalez : Salez {
    /**
     * For handling Amazon IAP purchases. Purchases can exist from both and incomplete purchase flow,
     * such as when a user loses network connection after a purchase, or from a
     * normal purchase flow.
     */
    fun processPurchase(response: PurchaseResponse?)

    /**
     * For handling queries for either a recent list of purchases,
     * or for a complete history of purchases made.
     */
    fun processPurchaseUpdates(response: PurchaseUpdatesResponse?)

    /**
     * Helper method for a verified PurchaseUpdatesResponse
     */
    fun processOrdersQueryResult(response: PurchaseUpdatesResponse)

    /**
     * Helper method for a verified PurchaseUpdatesResponse
     */
    fun processHistoryQueryResult(response: PurchaseUpdatesResponse)
}
