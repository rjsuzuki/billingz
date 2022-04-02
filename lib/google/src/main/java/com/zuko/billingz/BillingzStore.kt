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
import androidx.collection.ArrayMap
import com.zuko.billingz.core.store.Storez
import com.zuko.billingz.core.store.model.Productz
import com.zuko.billingz.core.store.sales.Salez
import com.zuko.billingz.google.store.GoogleStore

object BillingzStore {

    class Builder : Storez.Builder {
        private lateinit var instance: Storez
        private lateinit var updaterListener: Salez.OrderUpdaterListener
        private lateinit var validatorListener: Salez.OrderValidatorListener
        private lateinit var products: ArrayMap<String, Productz.Type>
        private var accountId: String? = null
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

        override fun setProducts(products: ArrayMap<String, Productz.Type>): Storez.Builder {
            this.products = products
            return this
        }

        override fun build(context: Context?): Storez {
            instance = GoogleStore.Builder()
                .setOrderUpdater(updaterListener)
                .setOrderValidator(validatorListener)
                .setAccountId(accountId)
                .build(context)
            return instance
        }
    }
}
