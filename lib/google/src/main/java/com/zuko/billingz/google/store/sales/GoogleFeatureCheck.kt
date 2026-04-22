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

package com.zuko.billingz.google.store.sales

import com.android.billingclient.api.BillingClient
import com.zuko.billingz.core.misc.Logger

object GoogleFeatureCheck {

    private const val TAG = "BillingzGoogleFeature"

    internal fun logSupportedFeatures(billingClient: BillingClient?) {
        Logger.d(
            TAG,
            "[Feature check] Are subscriptions supported: ${isFeatureSupported(billingClient, BillingClient.FeatureType.SUBSCRIPTIONS)}"
        )
        Logger.d(
            TAG,
            "[Feature check] Are price change confirmations supported: ${isFeatureSupported(billingClient, BillingClient.FeatureType.PRICE_CHANGE_CONFIRMATION)}"
        )
        Logger.d(
            TAG,
            "[Feature check] Are in-app messages supported: ${isFeatureSupported(billingClient, BillingClient.FeatureType.IN_APP_MESSAGING)}"
        )

        Logger.d(
            TAG,
            "[Feature check] Are billing configs supported: ${isFeatureSupported(billingClient, BillingClient.FeatureType.BILLING_CONFIG)}"
        )
        Logger.d(
            TAG,
            "[Feature check] Are external offers supported: ${isFeatureSupported(billingClient, BillingClient.FeatureType.EXTERNAL_OFFER)}"
        )
        Logger.d(
            TAG,
            "[Feature check] Are alternative billing methods supported: ${isFeatureSupported(billingClient, BillingClient.FeatureType.ALTERNATIVE_BILLING_ONLY)}"
        )
        Logger.d(
            TAG,
            "[Feature check] Are product details supported: ${isFeatureSupported(billingClient, BillingClient.FeatureType.PRODUCT_DETAILS)}"
        )
        Logger.d(
            TAG,
            "[Feature check] Are subscription updates supported: ${isFeatureSupported(billingClient, BillingClient.FeatureType.SUBSCRIPTIONS_UPDATE)}"
        )
    }

    private fun isFeatureSupported(billingClient: BillingClient?, featureType: String): Boolean {
        try {
            // Ensure the testing feature is enabled before trying to use it.
            val isFeatureSupported = billingClient?.isFeatureSupported(featureType)
            return isFeatureSupported != null && isFeatureSupported.responseCode == BillingClient.BillingResponseCode.OK
        } catch (e: Exception) {
            Logger.e(TAG, "isFeatureSupported failed", e)
        }
        return false
    }
}
