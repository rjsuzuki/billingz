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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.zuko.billingz.core.store.model.QueryResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Simulated [QueryResult] backed by both a [MutableLiveData] and a [MutableStateFlow], mirroring how
 * the real Google/Amazon query results publish to UI (LiveData) and non-UI (StateFlow) observers.
 */
class SimQueryResult<T>(initial: T? = null) : QueryResult<T> {

    private val live = MutableLiveData(initial)
    private val state = MutableStateFlow(initial)

    override fun liveData(): LiveData<out T?> = live

    override fun flow(): StateFlow<T?> = state

    /** Emit a new value to every observer. Safe to call from the main thread. */
    fun post(value: T?) {
        live.value = value
        state.value = value
    }
}
