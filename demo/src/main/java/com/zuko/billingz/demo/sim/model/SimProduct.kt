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

import com.zuko.billingz.core.store.model.PricingInfo
import com.zuko.billingz.core.store.model.Productz
import com.zuko.billingz.demo.sim.StoreType
import java.util.Currency
import java.util.Locale

/**
 * In-memory [Productz] used by the simulation. Stands in for a Google `ProductDetails` or an
 * Amazon `Product` so the demo never has to query a real store catalog.
 */
data class SimProduct(
    private val productId: String,
    private val name: String,
    private val title: String,
    private val description: String,
    private val price: String,
    override val type: Productz.Type,
    private val storeType: StoreType,
    private val promotion: Productz.Promotion = Productz.Promotion.NONE,
    private val currency: Currency = Currency.getInstance(Locale.getDefault())
) : Productz {

    override fun getProductId(): String = productId

    override fun getName(): String = name

    override fun getTitle(): String = title

    override fun getPrice(): String = price

    override fun getDescription(): String = description

    override fun getIconUrl(): String? = null

    override fun getPromotion(): Productz.Promotion = promotion

    // Amazon IAP does not expose pricing info, so the simulation returns null for it,
    // matching the real Amazon module's behavior.
    override fun getPricingInfo(): PricingInfo? = null

    override fun getCurrency(): Currency = currency

    override fun isGoogle(): Boolean = storeType == StoreType.GOOGLE

    override fun isAmazon(): Boolean = storeType == StoreType.AMAZON
}
