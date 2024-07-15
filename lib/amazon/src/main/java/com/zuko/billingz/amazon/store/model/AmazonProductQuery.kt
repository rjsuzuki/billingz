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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.zuko.billingz.core.store.model.Productz
import com.zuko.billingz.core.store.model.QueryResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AmazonProductQuery(
    private val sku: String,
    private val type: Productz.Type
) : QueryResult<AmazonProduct> {

    val queriedProductLiveData = MutableLiveData<AmazonProduct?>()
    val queriedProductStateFlow: MutableStateFlow<AmazonProduct?> = MutableStateFlow(null)
    private val queriedProductState = queriedProductStateFlow.asStateFlow()

    override fun liveData(): LiveData<AmazonProduct?> {
        return queriedProductLiveData
    }

    override fun flow(): StateFlow<AmazonProduct?> {
        return queriedProductState
    }
}
