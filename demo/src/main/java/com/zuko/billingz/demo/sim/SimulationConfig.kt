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

import com.zuko.billingz.core.store.model.Orderz

/**
 * The store response the simulation should produce for the next purchase flow.
 *
 * These map onto the lifecycle states defined by [Orderz.State] / [Orderz.Result] so the
 * demo can drive the library's purchase flow into every outcome a real store could return.
 */
enum class PurchaseOutcome(val label: String) {
    /** A normal, fully successful purchase. Routes through validation and completes. */
    SUCCESS("Success"),

    /** The user dismisses the store's purchase dialog. */
    CANCELED("User canceled"),

    /** The billing service rejects the purchase (e.g. payment declined). */
    FAILED("Failed / error"),

    /** A deferred/slow form of payment that resolves later (Google "PENDING"). */
    PENDING("Pending"),

    /** The user already owns a non-consumable / active subscription. */
    ALREADY_OWNED("Already owned");
}

/**
 * Snapshot of everything that controls how [SimulatedStore] behaves. Held in a
 * [kotlinx.coroutines.flow.StateFlow] by the demo so the UI can mutate it live between actions.
 *
 * @param storeType which backend ([StoreType]) the simulation imitates.
 * @param purchaseOutcome the result the next [com.zuko.billingz.core.store.agent.Agentz.startOrder] produces.
 * @param validatorApproves whether the demo's [com.zuko.billingz.core.store.sales.Salez.OrderValidatorListener]
 *   approves the order. Only relevant for [PurchaseOutcome.SUCCESS] — set false to exercise the
 *   "validation rejected" branch that a developer's backend check would trigger.
 * @param autoConnectOnResume mimics a store that reconnects automatically on resume.
 * @param latencyMs artificial delay applied to async operations so loading states are observable.
 */
data class SimulationConfig(
    val storeType: StoreType = StoreType.GOOGLE,
    val purchaseOutcome: PurchaseOutcome = PurchaseOutcome.SUCCESS,
    val validatorApproves: Boolean = true,
    val autoConnectOnResume: Boolean = true,
    val latencyMs: Long = 600L
)
