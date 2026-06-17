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
package com.zuko.billingz.demo

import android.app.Activity
import androidx.lifecycle.LiveData
import com.zuko.billingz.core.store.Storez
import com.zuko.billingz.core.store.agent.Agentz
import com.zuko.billingz.core.store.client.Clientz
import com.zuko.billingz.core.store.model.OrderHistoryz
import com.zuko.billingz.core.store.model.Orderz
import com.zuko.billingz.core.store.model.Productz
import com.zuko.billingz.core.store.model.Receiptz
import com.zuko.billingz.core.store.sales.Salez
import com.zuko.billingz.demo.sim.SimulatedStore
import com.zuko.billingz.demo.sim.SimulationConfig
import com.zuko.billingz.demo.sim.TestCatalog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Drives the demo against the library's public [Agentz] facade. This is the "integrating app" half
 * of the demo: it implements the two listeners every consumer must provide
 * ([Salez.OrderUpdaterListener] and [Salez.OrderValidatorListener]) and exposes observable state for
 * the Compose UI.
 *
 * The store behind [agent] is a [SimulatedStore]. To run against a real billing service, replace the
 * builder below with `com.zuko.billingz.google.BillingzStore.Builder` (add `:lib:google` to the demo
 * dependencies) — nothing else here changes, because everything is expressed through [Agentz].
 */
class DemoController {

    private val configState = MutableStateFlow(SimulationConfig())

    /** Live simulation settings the UI can mutate between actions. */
    val config: StateFlow<SimulationConfig> = configState

    private val eventState = MutableStateFlow<List<String>>(emptyList())

    /** Human-readable, newest-last log of every store + listener event. */
    val events: StateFlow<List<String>> = eventState

    /** The developer-supplied validation hook. Approves or rejects based on the current config. */
    private val orderValidator = object : Salez.OrderValidatorListener {
        override fun validate(order: Orderz, callback: Salez.ValidatorCallback) {
            if (configState.value.validatorApproves) {
                callback.validated(order)
            } else {
                callback.invalidated(order)
            }
        }
    }

    /** The developer-supplied result callbacks for the purchase flow. */
    private val orderUpdater = object : Salez.OrderUpdaterListener {
        override fun onComplete(receipt: Receiptz) {
            log("listener.onComplete — ${receipt.skus?.joinToString()} (order ${receipt.orderId})")
        }

        override fun onFailure(order: Orderz) {
            log("listener.onFailure — ${order.result} (${order.resultMessage})")
        }

        override fun onCanceled(order: Orderz) {
            log("listener.onCanceled — ${order.orderId}")
        }
    }

    private val store: Storez = SimulatedStore.Builder()
        .setOrderUpdater(orderUpdater)
        .setOrderValidator(orderValidator)
        .setEventLogger(::log)
        .setConfigProvider { configState.value }
        .setCatalogProvider(TestCatalog::storeProducts)
        .build(null)

    /** The library facade, identical to what a real `BillingzStore` would expose. */
    val agent: Agentz = store.getAgent()

    /** Register this with a `Lifecycle` so connection/teardown follow the activity. */
    val lifecycleObserver: Storez get() = store

    val connectionState: LiveData<Clientz.ConnectionStatus> = agent.getState()
    val inventoryReady: StateFlow<Boolean> = agent.isInventoryReadyStateFlow()

    /** Products returned by the most recent inventory query. */
    val products: StateFlow<Map<String, Productz>?> =
        agent.queryInventory(TestCatalog.requestMap()).flow()

    /** The owned-items history, refreshed as purchases complete. */
    val receipts: StateFlow<OrderHistoryz?> = agent.queryReceipts(null).flow()

    fun setStoreType(transform: (SimulationConfig) -> SimulationConfig) {
        configState.value = transform(configState.value)
    }

    fun buy(activity: Activity, productId: String) {
        agent.startOrder(activity, productId, null)
    }

    fun refreshInventory() {
        agent.queryInventory(TestCatalog.requestMap())
    }

    fun refreshReceipts() {
        agent.queryReceipts(null)
    }

    fun clearLog() {
        eventState.value = emptyList()
    }

    private fun log(message: String) {
        eventState.value = eventState.value + message
    }
}
