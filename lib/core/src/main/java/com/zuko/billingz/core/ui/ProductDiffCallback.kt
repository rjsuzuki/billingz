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
package com.zuko.billingz.core.ui

import androidx.recyclerview.widget.DiffUtil
import com.zuko.billingz.core.store.model.Productz

/**
 * Implementation of [DiffUtil.Callback] for a mutable list of [Productz] objects.
 * @constructor
 * @param oldList
 * @param newList
 */
class ProductDiffCallback(private val oldList: MutableList<Productz>, private val newList: MutableList<Productz>) : DiffUtil.Callback() {

    override fun getOldListSize(): Int {
        return oldList.size
    }

    override fun getNewListSize(): Int {
        return newList.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].getProductId == newList[newItemPosition].getProductId
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].description == newList[newItemPosition].description &&
            oldList[oldItemPosition].title == newList[newItemPosition].title &&
            oldList[oldItemPosition].price == newList[newItemPosition].price &&
            oldList[oldItemPosition].getProductId == newList[newItemPosition].getProductId &&
            oldList[oldItemPosition].type == newList[newItemPosition].type
    }
}
