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
package com.zuko.billingz.google.store.sales

import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.zuko.billingz.lib.store.sales.Order

/**
 * Represents an Order.
 * @property billingResult
 * @property purchase
 * @property msg - shorthand for message.
 */
interface GoogleOrder: Order {

    val billingResult: BillingResult?
    val purchase: Purchase?
    val msg: String

}


