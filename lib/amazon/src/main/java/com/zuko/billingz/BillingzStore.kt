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

package com.zuko.billingz

import android.content.Context
import com.zuko.billingz.amazon.store.AmazonStore
import com.zuko.billingz.core.store.Storez
import com.zuko.billingz.core.store.sales.Salez

object BillingzStore {

    class Builder : Storez.Builder {
        private lateinit var instance: Storez
        private lateinit var updaterListener: Salez.OrderUpdaterListener
        private lateinit var validatorListener: Salez.OrderValidatorListener
        private var accountId: String? = null
        private var profileId: String? = null
        private var hashingSalt: String? = null
        private var isNewVersion = false
        private var isDebug = false

        /**
         * @param listener - Required to be set for proper functionality
         */
        override fun setOrderUpdater(listener: Salez.OrderUpdaterListener): Storez.Builder {
            updaterListener = listener
            return this
        }

        /**
         * @param listener - Required to be set for proper functionality
         */
        override fun setOrderValidator(listener: Salez.OrderValidatorListener): Storez.Builder {
            validatorListener = listener
            return this
        }

        /**
         * Not used in this implementation
         */
        override fun setAccountId(id: String?): Storez.Builder {
            accountId = id
            return this
        }

        override fun setProfileId(id: String?): Storez.Builder {
            profileId = id
            return this
        }

        override fun setObfuscatingHashingSalt(salt: String?): Storez.Builder {
            hashingSalt = salt
            return this
        }

        override fun setNewVersion(enable: Boolean): Storez.Builder {
            isNewVersion = enable
            return this
        }

        override fun enableDebugLogs(enable: Boolean): Storez.Builder {
            isDebug = enable
            return this
        }

        override fun build(context: Context?): Storez {
            instance = AmazonStore.Builder()
                .setOrderUpdater(updaterListener)
                .setOrderValidator(validatorListener)
                .setAccountId(accountId)
                .setProfileId(profileId)
                .setObfuscatingHashingSalt(hashingSalt)
                .setNewVersion(isNewVersion)
                .enableDebugLogs(isDebug)
                .build(context)
            return instance
        }
    }
}
