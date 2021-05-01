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
package com.zuko.billingz.lib.store.products

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.SkuDetails
import com.zuko.billingz.lib.LogUtil
import com.zuko.billingz.lib.store.sales.Order
import kotlinx.coroutines.CoroutineScope

/**
 * Represents a consumable product.
 * @property id
 * @property sku
 * @property name
 * @property price
 * @property description
 * @property details
 */
data class Consumable(
    override var id: Int? = 0,
    override var sku: String? = null,
    override var name: String? = null,
    override var price: String? = null,
    override var description: String? = null,
    override var details: SkuDetails? = null
) : Product {

    override val skuType: String = BillingClient.SkuType.INAPP
    override val type: Product.ProductType = Product.ProductType.CONSUMABLE

    companion object {
        private const val TAG = "Consumable"

        fun completeOrder(
            billingClient: BillingClient?,
            purchase: Purchase,
            order: MutableLiveData<Order>,
            mainScope: CoroutineScope?
        ) {

            val consumeParams = ConsumeParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()

            billingClient?.consumeAsync(consumeParams) { billingResult, p ->
                val msg: String
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    msg = "Consumable successfully purchased: $p"
                    Log.d(TAG, "Product successfully purchased: ${purchase.sku}")
                } else {
                    msg = billingResult.debugMessage
                    LogUtil.log.e(TAG, "Error purchasing consumable. $msg")
                }
                val data = Order(
                    purchase = purchase,
                    billingResult = billingResult,
                    msg = msg
                )
                order.postValue(data)
            }
        }
    }
}
