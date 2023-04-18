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
package com.zuko.billingz.core.store.client

import android.content.Context
import androidx.annotation.UiThread
import androidx.lifecycle.MutableLiveData
import com.zuko.billingz.core.misc.CleanUpz

/**
 * Blueprint of the core logic of the library.
 */
interface Clientz : CleanUpz {
    /**
     *
     */
    var connectionState: MutableLiveData<ConnectionStatus>

    /**
     * @return Boolean
     * Checks if the client has been initialized yet
     */
    @UiThread
    fun initialized(): Boolean

    /**
     * @return Boolean
     * Checks if the client properly connected to the android billing api,
     * so that requests to other methods will succeed.
     */
    @UiThread
    fun isReady(): Boolean

    /**
     * Initialize the Android Billing Library
     * INTERNAL USE ONLY
     * @param context
     * @param connectionListener
     */
    @UiThread
    fun init(
        context: Context?,
        connectionListener: ConnectionListener
    )

    /**
     * Starts connection to GooglePlay
     * INTERNAL USE ONLY
     */
    @UiThread
    fun connect()

    /**
     * Stops connection to GooglePlay
     * INTERNAL USE ONLY
     */
    @UiThread
    fun disconnect()

    /**
     * Verifies connection to the billing service
     */
    fun checkConnection()

    /**
     * Callback used to respond to a successful connection.
     * INTERNAL USE ONLY
     */
    interface ConnectionListener {
        fun connected()
    }

    /**
     * Interface for reconnection logic to the billing service
     * INTERNAL USE ONLY
     */
    interface ReconnectListener { // todo

        /**
         *
         */
        @UiThread
        fun retry()

        /**
         *
         */
        fun cancel()
    }

    enum class ConnectionStatus {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        CLOSED
    }
}
