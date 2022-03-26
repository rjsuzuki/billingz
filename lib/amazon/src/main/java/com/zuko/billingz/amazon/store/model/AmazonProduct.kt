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
package com.zuko.billingz.amazon.store.model

import com.amazon.device.iap.model.Product
import com.amazon.device.iap.model.ProductType
import com.zuko.billingz.core.store.model.PricingInfo
import com.zuko.billingz.core.store.model.Productz
import java.util.Currency
import java.util.Locale

data class AmazonProduct(var amazonProduct: Product) : Productz {

    override var sku: String? = amazonProduct.sku
    override var title: String? = amazonProduct.title
    override var price: String? = amazonProduct.price
    override var description: String? = amazonProduct.description
    override var iconUrl: String? = amazonProduct.smallIconUrl
    override val type: Productz.Type = when (amazonProduct.productType) {
        ProductType.CONSUMABLE -> Productz.Type.CONSUMABLE
        ProductType.ENTITLED -> Productz.Type.NON_CONSUMABLE
        ProductType.SUBSCRIPTION -> Productz.Type.SUBSCRIPTION
        else -> Productz.Type.UNKNOWN
    }
    override val promotion: Productz.Promotion = Productz.Promotion.NONE
    override val pricingInfo: PricingInfo? = null

    override fun getCurrency(): Currency {
        return Currency.getInstance(Locale.getDefault())
    }

    override fun isAmazon(): Boolean {
        return true
    }

    override fun isGoogle(): Boolean {
        return false
    }
}
