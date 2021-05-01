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
package com.zuko.billingz.ui

import androidx.recyclerview.widget.DiffUtil
import com.zuko.billingz.lib.store.products.Product

/**
 * Implementation of [DiffUtil.Callback] for a mutable list of [Product] objects.
 * @constructor
 * @param oldList
 * @param newList
 */
class ProductDiffCallback(private val oldList: MutableList<Product>, private val newList: MutableList<Product>) : DiffUtil.Callback() {

    override fun getOldListSize(): Int {
        return oldList.size
    }

    override fun getNewListSize(): Int {
        return newList.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].id == newList[newItemPosition].id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].description == newList[newItemPosition].description &&
            oldList[oldItemPosition].details == newList[newItemPosition].details &&
            oldList[oldItemPosition].name == newList[newItemPosition].name &&
            oldList[oldItemPosition].price == newList[newItemPosition].price &&
            oldList[oldItemPosition].sku == newList[newItemPosition].sku &&
            oldList[oldItemPosition].skuType == newList[newItemPosition].skuType &&
            oldList[oldItemPosition].type == newList[newItemPosition].type
    }
}
