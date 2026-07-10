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
package com.zuko.billingz.demo.sim

import com.zuko.billingz.core.store.model.Productz
import com.zuko.billingz.demo.sim.model.SimProduct

/**
 * A static set of test SKUs spanning every [Productz.Type] the library supports. These are not real
 * Play Console / Amazon products — they exist only to drive the simulated purchase flows.
 */
object TestCatalog {

    /** One catalog entry. The same definition is used to build the app's query request and the
     * detailed product the simulated store "returns". */
    data class TestSku(
        val id: String,
        val name: String,
        val title: String,
        val description: String,
        val price: String,
        val type: Productz.Type
    )

    val skus: List<TestSku> = listOf(
        TestSku(
            id = "sim_consumable_coins_100",
            name = "100 Coins",
            title = "100 Coins (Consumable)",
            description = "A consumable pack of coins. Can be purchased repeatedly.",
            price = "$0.99",
            type = Productz.Type.CONSUMABLE
        ),
        TestSku(
            id = "sim_consumable_gems_500",
            name = "500 Gems",
            title = "500 Gems (Consumable)",
            description = "A larger consumable pack of gems.",
            price = "$4.99",
            type = Productz.Type.CONSUMABLE
        ),
        TestSku(
            id = "sim_nonconsumable_remove_ads",
            name = "Remove Ads",
            title = "Remove Ads (Non-consumable)",
            description = "A one-time entitlement that can only be owned once.",
            price = "$2.99",
            type = Productz.Type.NON_CONSUMABLE
        ),
        TestSku(
            id = "sim_subscription_pro_monthly",
            name = "Pro Monthly",
            title = "Pro Monthly (Subscription)",
            description = "A recurring monthly subscription.",
            price = "$9.99 / month",
            type = Productz.Type.SUBSCRIPTION
        )
    )

    /** The sku → type map an app passes to [com.zuko.billingz.core.store.agent.Agentz.queryInventory]. */
    fun requestMap(): Map<String, Productz.Type> = skus.associate { it.id to it.type }

    /** The detailed [Productz] objects the simulated store "returns" for a given [StoreType]. */
    fun storeProducts(storeType: StoreType): List<Productz> = skus.map { sku ->
        SimProduct(
            productId = sku.id,
            name = sku.name,
            title = sku.title,
            description = sku.description,
            price = sku.price,
            type = sku.type,
            storeType = storeType
        )
    }
}
