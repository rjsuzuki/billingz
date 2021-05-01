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
package com.zuko.billingz.lib.store.sales

import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchaseHistoryResponseListener
import com.zuko.billingz.lib.misc.CleanUpListener


/**
 * Blueprint for managing the order history of a user
 */
interface History : CleanUpListener {

    /**
     *
     * Fetch all purchases to keep history up-to-date.
     * Network issues, multiple devices, and external purchases
     * could create untracked purchases - call this method in the
     * onCreate and onResume lifecycle events.
     */
    fun refreshPurchaseHistory(sales: Sales)

    /**
     * @param sales
     */
    fun queryPurchases(sales: Sales)

    /**
     * @param skuType
     * @param listener
     */
    fun queryPurchaseHistory(skuType: String, listener: PurchaseHistoryResponseListener)

    /**
     * @return - mutable list of active subscriptions
     */
    fun getOwnedSubscriptions(): MutableList<Purchase>

    /**
     * @return - mutable list of active in-app purchases
     */
    fun getOwnedInAppProducts(): MutableList<Purchase>
}
