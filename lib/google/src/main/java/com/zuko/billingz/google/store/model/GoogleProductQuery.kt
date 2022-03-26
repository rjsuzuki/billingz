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

package com.zuko.billingz.google.store.model

import androidx.lifecycle.LiveData
import com.zuko.billingz.core.store.model.Productz
import com.zuko.billingz.core.store.model.QueryResult
import com.zuko.billingz.google.store.inventory.GoogleInventory
import kotlinx.coroutines.flow.StateFlow

data class GoogleProductQuery(
    private val sku: String,
    private val type: Productz.Type,
    private val inventory: GoogleInventory
) : QueryResult<Productz> {

    override fun liveData(): LiveData<Productz?> {
        return inventory.queryProductLiveData()
    }

    override fun flow(): StateFlow<Productz?> {
        return inventory.queryProductStateFlow()
    }

    companion object {
        private const val TAG = "GoogleProductQuery"
    }
}
