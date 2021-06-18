package com.zuko.billingz.amazon.store.products

import com.zuko.billingz.lib.store.products.Product

data class Subscription(override var id: Int? = 0,
                        override var sku: String? = null,
                        override var name: String? = null,
                        override var price: String? = null,
                        override var description: String? = null,
                        override var iconUrl: String?,
                        override val type: Product.Type,
                        override val promotion: Product.Promotion
): AmazonProduct
