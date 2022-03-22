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

package com.zuko.billingz.core.store.model

import androidx.annotation.UiThread
import androidx.lifecycle.LiveData
import kotlinx.coroutines.flow.StateFlow

/**
 * Functionality for query results to be published as android LiveData objects
 * or kotlin StateFlow objects. Both provide functionality for multiple subscribers.
 */
interface QueryResult <out T> {
    /**
     * Recommended for use-cases related to the ui (activity or fragment)
     */
    @UiThread
    fun liveData(): LiveData<out T?>

    /**
     * Recommended for non-ui related use-cases.
     */
    fun flow(): StateFlow<T?>
}