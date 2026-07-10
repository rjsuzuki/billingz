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
package com.zuko.billingz.demo.sim

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.zuko.billingz.core.store.Storez
import com.zuko.billingz.core.store.agent.Agentz
import com.zuko.billingz.core.store.client.Clientz
import com.zuko.billingz.core.store.model.OrderHistoryz
import com.zuko.billingz.core.store.model.Orderz
import com.zuko.billingz.core.store.model.Productz
import com.zuko.billingz.core.store.model.QueryResult
import com.zuko.billingz.core.store.sales.Salez
import com.zuko.billingz.demo.sim.model.SimOrder
import com.zuko.billingz.demo.sim.model.SimOrderHistory
import com.zuko.billingz.demo.sim.model.SimProduct
import com.zuko.billingz.demo.sim.model.SimQueryResult
import com.zuko.billingz.demo.sim.model.SimReceipt
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Date

/**
 * A fake [Storez] / [Agentz] implementation that imitates Google Play / Amazon Appstore responses
 * entirely in memory. It lets the demo exercise the library's public purchase contract — connection
 * state, inventory queries, the order → validate → complete lifecycle, and the
 * [Salez.OrderUpdaterListener] / [Salez.OrderValidatorListener] callbacks — without ever touching a
 * real billing service or requiring a signed-in account.
 *
 * Behavior is driven by a [SimulationConfig] read fresh on every action, so the UI can change the
 * simulated outcome between taps. To test against a real store instead, swap [Builder] for
 * `com.zuko.billingz.google.BillingzStore.Builder` (or the Amazon variant) — the rest of the demo,
 * which only depends on [Agentz], is unaffected.
 */
class SimulatedStore internal constructor(
    private val configProvider: () -> SimulationConfig,
    private val catalogProvider: (StoreType) -> List<Productz>,
    private val logger: (String) -> Unit
) : Storez {

    private val scope = MainScope()
    private var orderSequence = 0

    private var updaterListener: Salez.OrderUpdaterListener? = null
    private var validatorListener: Salez.OrderValidatorListener? = null

    private val connectionState = MutableLiveData(Clientz.ConnectionStatus.DISCONNECTED)
    private val inventoryReadyLive = MutableLiveData(false)
    private val inventoryReadyFlow = MutableStateFlow(false)

    /** SKUs successfully loaded by [queryInventory]; a purchase can only start for one of these. */
    private val inventory = LinkedHashMap<String, Productz>()

    /** Completed purchases keyed by entitlement id, the simulated user's owned items. */
    private val ownedReceipts = LinkedHashMap<String, SimReceipt>()

    private val currentOrder = MutableLiveData<Orderz>()
    private val inventoryResult = SimQueryResult<Map<String, Productz>>(emptyMap())
    private val receiptsResult = SimQueryResult<OrderHistoryz>()
    private val ordersResult = SimQueryResult<Orderz>()

    private val config: SimulationConfig get() = configProvider()

    // region Storez / lifecycle ---------------------------------------------------------------

    override fun getAgent(): Agentz = agent

    override fun init(context: Context?) {
        logger("init() — simulated ${config.storeType} store")
    }

    override fun create() {
        logger("create()")
        connect()
    }

    override fun start() {
        logger("start()")
    }

    override fun resume() {
        logger("resume()")
        if (config.autoConnectOnResume && connectionState.value != Clientz.ConnectionStatus.CONNECTED) {
            connect()
        }
    }

    override fun pause() {
        logger("pause()")
    }

    override fun stop() {
        logger("stop()")
    }

    override fun destroy() {
        logger("destroy()")
        connectionState.value = Clientz.ConnectionStatus.CLOSED
        scope.cancel()
    }

    // DefaultLifecycleObserver hooks so the store reacts when registered with a LifecycleOwner.
    override fun onCreate(owner: LifecycleOwner) = create()
    override fun onStart(owner: LifecycleOwner) = start()
    override fun onResume(owner: LifecycleOwner) = resume()
    override fun onPause(owner: LifecycleOwner) = pause()
    override fun onStop(owner: LifecycleOwner) = stop()
    override fun onDestroy(owner: LifecycleOwner) = destroy()

    // endregion

    // region connection -----------------------------------------------------------------------

    private fun connect() {
        if (connectionState.value == Clientz.ConnectionStatus.CONNECTED) return
        connectionState.value = Clientz.ConnectionStatus.CONNECTING
        logger("connecting to ${config.storeType} billing service…")
        scope.launch {
            delay(config.latencyMs)
            connectionState.value = Clientz.ConnectionStatus.CONNECTED
            logger("connected")
        }
    }

    private fun disconnect() {
        connectionState.value = Clientz.ConnectionStatus.DISCONNECTED
        logger("disconnected")
    }

    // endregion

    private val agent = object : Agentz {

        override fun updateIdentifiers(accountId: String?, profileId: String?, hashingSalt: String?) {
            logger("updateIdentifiers(account=$accountId, profile=$profileId)")
        }

        override fun isInventoryReadyLiveData(): LiveData<Boolean> = inventoryReadyLive

        override fun isInventoryReadyStateFlow(): StateFlow<Boolean> = inventoryReadyFlow

        override fun getState(): LiveData<Clientz.ConnectionStatus> = connectionState

        override fun queryInventory(products: Map<String, Productz.Type>): QueryResult<Map<String, Productz>> {
            logger("queryInventory(${products.size} skus)")
            inventoryReadyLive.value = false
            inventoryReadyFlow.value = false
            scope.launch {
                delay(config.latencyMs)
                inventory.clear()
                val catalog = catalogProvider(config.storeType).associateBy { it.getProductId() }
                products.forEach { (sku, type) ->
                    val product = catalog[sku]
                    if (product != null && product.type == type) {
                        inventory[sku] = product
                    }
                }
                inventoryResult.post(inventory.toMap())
                inventoryReadyLive.value = true
                inventoryReadyFlow.value = true
                logger("inventory ready — ${inventory.size} product(s) loaded")
            }
            return inventoryResult
        }

        override fun queryProduct(sku: String, type: Productz.Type): QueryResult<Productz> {
            logger("queryProduct($sku, $type)")
            val result = SimQueryResult<Productz>()
            scope.launch {
                delay(config.latencyMs)
                val product = catalogProvider(config.storeType)
                    .firstOrNull { it.getProductId() == sku && it.type == type }
                result.post(product)
            }
            return result
        }

        override fun startOrder(activity: Activity?, productId: String?, options: Bundle?): LiveData<Orderz> {
            logger("startOrder($productId)")
            val product = inventory[productId]
            if (product == null) {
                // Mirrors GoogleStore.startOrder: an order cannot start for an un-queried sku.
                val order = newOrder(productId ?: "unknown").apply {
                    state = Orderz.State.FAILED
                    result = Orderz.Result.INVALID_PRODUCT
                    resultMessage = "Product '$productId' not found. Call queryInventory first."
                }
                currentOrder.value = order
                logger("✗ ${order.resultMessage}")
                updaterListener?.onFailure(order)
                return currentOrder
            }
            val order = newOrder(product.getProductId() ?: productId ?: "unknown")
            currentOrder.value = order
            scope.launch {
                delay(config.latencyMs)
                resolveOutcome(order, product)
            }
            return currentOrder
        }

        override fun queryReceipts(type: Productz.Type?): QueryResult<OrderHistoryz> {
            logger("queryReceipts(${type ?: "ALL"})")
            scope.launch {
                delay(config.latencyMs)
                publishReceipts()
            }
            return receiptsResult
        }

        override fun queryOrders(): QueryResult<Orderz> {
            logger("queryOrders()")
            scope.launch {
                delay(config.latencyMs)
                ordersResult.post(currentOrder.value)
            }
            return ordersResult
        }

        override fun completeOrder(order: Orderz) {
            logger("completeOrder(${order.orderId})")
            (order as? SimOrder)?.let { finalize(it) }
        }

        override fun cancelOrder(order: Orderz) {
            logger("cancelOrder(${order.orderId})")
            (order as? SimOrder)?.let {
                it.state = Orderz.State.CANCELED
                it.result = Orderz.Result.USER_CANCELED
                it.resultMessage = "Order canceled"
                currentOrder.value = it
                updaterListener?.onCanceled(it)
            }
        }

        @Deprecated("Will be removed in a future release.")
        override fun getProduct(sku: String?): Productz? = inventory[sku]

        @Deprecated("Will be removed in a future release")
        override fun getProducts(type: Productz.Type?, promo: Productz.Promotion?): Map<String, Productz> {
            return inventory.filterValues { p ->
                (type == null || p.type == type) && (promo == null || p.getPromotion() == promo)
            }
        }
    }

    // region purchase state machine -----------------------------------------------------------

    private fun resolveOutcome(order: SimOrder, product: Productz) {
        when (config.purchaseOutcome) {
            PurchaseOutcome.CANCELED -> {
                order.state = Orderz.State.CANCELED
                order.result = Orderz.Result.USER_CANCELED
                order.resultMessage = "User canceled the purchase"
                currentOrder.value = order
                logger("✗ purchase canceled")
                updaterListener?.onCanceled(order)
            }
            PurchaseOutcome.FAILED -> {
                order.state = Orderz.State.FAILED
                order.result = Orderz.Result.ERROR
                order.resultMessage = "Purchase failed"
                currentOrder.value = order
                logger("✗ purchase failed")
                updaterListener?.onFailure(order)
            }
            PurchaseOutcome.PENDING -> {
                order.state = Orderz.State.PENDING
                order.result = Orderz.Result.PENDING
                order.resultMessage = "Purchase pending — awaiting payment"
                currentOrder.value = order
                logger("⧗ purchase pending")
            }
            PurchaseOutcome.ALREADY_OWNED -> {
                order.state = Orderz.State.FAILED
                order.result = Orderz.Result.PRODUCT_ALREADY_OWNED
                order.resultMessage = "${product.getProductId()} is already owned"
                currentOrder.value = order
                logger("✗ already owned")
                updaterListener?.onFailure(order)
            }
            PurchaseOutcome.SUCCESS -> {
                order.state = Orderz.State.VALIDATING
                currentOrder.value = order
                logger("validating order…")
                val callback = object : Salez.ValidatorCallback {
                    override fun validated(order: Orderz) {
                        logger("validator approved")
                        (order as? SimOrder)?.let { finalize(it) }
                    }

                    override fun invalidated(order: Orderz) {
                        logger("✗ validator rejected")
                        (order as? SimOrder)?.let {
                            it.state = Orderz.State.FAILED
                            it.result = Orderz.Result.ERROR
                            it.resultMessage = "Order failed validation"
                            currentOrder.value = it
                            updaterListener?.onFailure(it)
                        }
                    }
                }
                val listener = validatorListener
                if (listener == null) {
                    // No developer validator supplied — complete directly.
                    finalize(order)
                } else {
                    listener.validate(order, callback)
                }
            }
        }
    }

    /** Acknowledge/consume the order, record the receipt, and notify the updater listener. */
    private fun finalize(order: SimOrder) {
        order.state = Orderz.State.COMPLETE
        order.result = Orderz.Result.SUCCESS
        order.resultMessage = "Purchase complete"
        currentOrder.value = order

        val entitlement = order.entitlement ?: order.orderId
        val receipt = SimReceipt(
            storeType = config.storeType,
            entitlement = entitlement,
            orderId = order.orderId,
            userId = "sim-user",
            skus = order.skus,
            orderDate = Date(),
            order = order
        )
        ownedReceipts[entitlement] = receipt
        publishReceipts()
        logger("✓ purchase complete — ${order.skus?.joinToString()}")
        updaterListener?.onComplete(receipt)
    }

    private fun publishReceipts() {
        receiptsResult.post(SimOrderHistory(config.storeType, ownedReceipts.toMap()))
    }

    private fun newOrder(sku: String): SimOrder {
        orderSequence += 1
        return SimOrder(
            sku = sku,
            storeType = config.storeType,
            orderId = "sim-order-$orderSequence",
            orderTime = System.currentTimeMillis(),
            entitlement = "sim-token-$orderSequence"
        )
    }

    // endregion

    /**
     * [Storez.Builder] for the simulation. Mirrors `BillingzStore.Builder` so the demo can construct
     * it the same way it would a real store, plus simulation-specific wiring.
     */
    class Builder : Storez.Builder {
        private var updater: Salez.OrderUpdaterListener? = null
        private var validator: Salez.OrderValidatorListener? = null
        private var configProvider: () -> SimulationConfig = { SimulationConfig() }
        private var catalogProvider: (StoreType) -> List<Productz> = { emptyList() }
        private var logger: (String) -> Unit = {}

        override fun setOrderUpdater(listener: Salez.OrderUpdaterListener): Builder {
            updater = listener
            return this
        }

        override fun setOrderValidator(listener: Salez.OrderValidatorListener): Builder {
            validator = listener
            return this
        }

        override fun setAccountId(id: String?): Builder = this
        override fun setProfileId(id: String?): Builder = this
        override fun setObfuscatingHashingSalt(salt: String?): Builder = this
        override fun setNewVersion(enable: Boolean): Builder = this
        override fun enableDebugLogs(enable: Boolean): Builder = this

        /** Supplies the live [SimulationConfig] read on every simulated action. */
        fun setConfigProvider(provider: () -> SimulationConfig) = apply { configProvider = provider }

        /** Supplies the product details the simulated store "returns" for each [StoreType]. */
        fun setCatalogProvider(provider: (StoreType) -> List<Productz>) = apply { catalogProvider = provider }

        /** Receives human-readable event log lines emitted by the store. */
        fun setEventLogger(logger: (String) -> Unit) = apply { this.logger = logger }

        override fun build(context: Context?): Storez {
            val store = SimulatedStore(configProvider, catalogProvider, logger)
            store.updaterListener = updater
            store.validatorListener = validator
            store.init(context)
            return store
        }
    }
}
