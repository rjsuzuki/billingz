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
package com.zuko.billingz.core.store

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver

/**
 * Implement androidx.lifecycle events
 */
interface StoreLifecycle : DefaultLifecycleObserver {

    fun init(context: Context?)

    /**
     * Initiate logic dependent on Android's onCreate() Lifecycle event.
     * If you added the Manager class as a lifecycleObserver, you do
     * not need to add this class manually in your activity/fragment.
     */
    fun create()

    /**
     * Initiate logic dependent on Android's onStart() Lifecycle event.
     * If you added the Manager class as a lifecycleObserver, you do
     * not need to add this class manually in your activity/fragment.
     */
    fun start()

    /**
     * Initiate logic dependent on Android's onResume() Lifecycle event.
     * If you added the Manager class as a lifecycleObserver, you do
     * not need to add this class manually in your activity/fragment.
     */
    fun resume()

    /**
     * Initiate logic dependent on Android's onPause() Lifecycle event.
     * If you added the Manager class as a lifecycleObserver, you do
     * not need to add this class manually in your activity/fragment.
     */
    fun pause()

    /**
     * Initiate logic dependent on Android's onStop() Lifecycle event.
     * If you added the Manager class as a lifecycleObserver, you do
     * not need to add this class manually in your activity/fragment.
     */
    fun stop()

    /**
     * Initiate logic dependent on Android's onDestroy() Lifecycle event.
     * If you added the Manager class as a lifecycleObserver, you do
     * not need to add this class manually in your activity/fragment.
     */
    fun destroy()
}
