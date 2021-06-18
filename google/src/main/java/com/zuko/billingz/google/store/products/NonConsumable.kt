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
package com.zuko.billingz.google.store.products

import androidx.lifecycle.MutableLiveData
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.AcknowledgePurchaseResponseListener
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.SkuDetails
import com.zuko.billingz.lib.store.products.Product
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Represents a non-consumable product.
 * AKA Entitlement product.
 * @property id
 * @property sku
 * @property name
 * @property price
 * @property description
 * @property details
 */
data class NonConsumable(
    override var id: Int? = 0,
    override var sku: String? = null,
    override var name: String? = null,
    override var price: String? = null,
    override var description: String? = null,
    override var details: SkuDetails? = null
) : Product {

    override val skuType: String = BillingClient.SkuType.INAPP
    override val type: Product.Type = Product.Type.NON_CONSUMABLE

    companion object {
        private const val TAG = "NonConsumable"

        fun completeOrder(
            billingClient: BillingClient?,
            purchase: Purchase,
            order: MutableLiveData<Order>,
            mainScope: CoroutineScope?
        ) {
            val listener = AcknowledgePurchaseResponseListener { billingResult ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    val data = Order(
                        purchase = purchase,
                        billingResult = billingResult,
                        msg = "Non-Consumable successfully purchased"
                    )
                    order.postValue(data)
                }
            }

            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                if (!purchase.isAcknowledged) {
                    val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken)
                    mainScope?.launch(Dispatchers.IO) {
                        billingClient?.acknowledgePurchase(acknowledgePurchaseParams.build(), listener)
                    }
                }
            }
        }
    }
}
