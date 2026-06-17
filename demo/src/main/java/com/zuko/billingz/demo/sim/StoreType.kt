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

/**
 * Which real billing backend the simulation should imitate.
 *
 * The library exposes the same [com.zuko.billingz.core.store.agent.Agentz] facade for both,
 * but the underlying stores differ in subtle ways (e.g. Amazon products do not expose
 * [com.zuko.billingz.core.store.model.Productz.getPricingInfo]). Selecting a [StoreType] lets the
 * demo reproduce those differences so app behavior can be validated against either.
 */
enum class StoreType {
    GOOGLE,
    AMAZON
}
