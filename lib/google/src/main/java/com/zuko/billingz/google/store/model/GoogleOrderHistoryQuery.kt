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
import com.zuko.billingz.core.store.model.QueryResult
import com.zuko.billingz.google.store.sales.GoogleSales
import kotlinx.coroutines.flow.StateFlow

class GoogleOrderHistoryQuery(private val sales: GoogleSales): QueryResult<GoogleOrderHistory> {
    override fun liveData(): LiveData<GoogleOrderHistory?> {
        return sales.queryReceiptsLiveData()
    }

    override fun flow(): StateFlow<GoogleOrderHistory?> {
        return sales.queryReceiptsStateFlow()
    }
}