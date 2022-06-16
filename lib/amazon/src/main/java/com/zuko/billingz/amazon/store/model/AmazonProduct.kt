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
import com.zuko.billingz.core.store.model.PricingInfo
import com.zuko.billingz.core.store.model.Productz
import java.util.Currency
import java.util.Locale

data class AmazonProduct(
    override val type: Productz.Type) : Productz {

    var id: Int = -1
    private var productId: String? = null
    private var title: String? = null
    private var price: String? = null
    private var description: String? = null
    private var iconUrl: String? = null
    private var promotion: Productz.Promotion = Productz.Promotion.NONE
    private var pricingInfo: PricingInfo? = null
    private var currency: Currency = Currency.getInstance(Locale.getDefault())

    @Suppress("unused")
    constructor(
        productId: String?,
        title: String?,
        description: String?,
        price: String?,
        iconUrl: String?,
        currency: Currency,
        pricingInfo: PricingInfo?,
        promotion: Productz.Promotion,
        type: Productz.Type
    ) : this(type) {
        this.productId = productId
        this.title = title
        this.description = description
        this.price = price
        this.iconUrl = iconUrl
        this.currency = currency
        this.pricingInfo = pricingInfo
        this.promotion = promotion
    }

    constructor(amazonProduct: Product, type: Productz.Type): this(type) {
        productId = amazonProduct.sku
        title = amazonProduct.title
        price = amazonProduct.price
        description = amazonProduct.description
        iconUrl = amazonProduct.smallIconUrl
    }

    override fun getProductId(): String? {
        return productId
    }

    override fun getName(): String? {
        return title
    }

    override fun getTitle(): String? {
        return title
    }

    override fun getPrice(): String? {
        return price
    }

    override fun getDescription(): String? {
       return description
    }

    override fun getIconUrl(): String? {
        return iconUrl
    }

    override fun getPromotion(): Productz.Promotion {
        return promotion
    }

    override fun getPricingInfo(): PricingInfo? {
        return pricingInfo
    }

    override fun getCurrency(): Currency {
        return currency
    }

    override fun isAmazon(): Boolean {
        return true
    }

    override fun isGoogle(): Boolean {
        return false
    }
}
