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
package com.zuko.billingz.core.store

import android.content.Context
import com.zuko.billingz.core.store.agent.Agentz
import com.zuko.billingz.core.store.sales.Salez

interface Storez : StoreLifecycle {

    /**
     * Returns the primary class for developers to conveniently
     * interact with one of the supported billing libraries, such as Android's Billing Library.
     * (Facade pattern)
     * @return [Agentz]
     */
    fun getAgent(): Agentz

    interface Builder {

        /**
         * @param listener - Required to be set for proper functionality
         */
        fun setOrderUpdater(listener: Salez.OrderUpdaterListener): Builder

        /**
         * @param listener - Required to be set for proper functionality
         */
        fun setOrderValidator(listener: Salez.OrderValidatorListener): Builder

        /**
         * Google Play can use it to detect irregular activity, such as many devices
         * making purchases on the same account in a short period of time.
         * @param - unique identifier for the user's account (64 character limit)
         * The account ID is obfuscated using SHA-256 encryption before being cached and used.
         */
        fun setAccountId(id: String?): Builder

        /**
         * Some applications allow users to have multiple profiles within a single account.
         * Use this method to send the user's profile identifier to Google.
         * @param - unique identifier for the user's profile (64 character limit).
         * The profile ID is obfuscated using SHA-256 before being cached and used.
         */
        fun setProfileId(id: String?): Builder

        /**
         * Specify a salt to use when obfuscating account id or profile id
         * @param - a string to use as salt for the hashing of identifiers
         */
        fun setObfuscatingHashingSalt(salt: String?): Builder

        /**
         * Toggle between an old and new version of Billingz.
         * Old versions maintain legacy functionality, and newer versions
         * use the latest changes from their respetive billing libraries.
         * Refer to the Billingz documntation to review current version differences in Google and Amazon.
         */
        fun setNewVersion(enable: Boolean): Builder

        /**
         * Enable debug logs
         */
        fun enableDebugLogs(enable: Boolean): Builder

        /**
         * Return an instance of [Storez] for either Google Play or Amazon Appstore
         */
        fun build(context: Context?): Storez
    }
}
