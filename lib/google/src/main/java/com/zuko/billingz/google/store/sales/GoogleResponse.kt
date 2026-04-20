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
package com.zuko.billingz.google.store.sales

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.zuko.billingz.core.misc.Logger

/**
 * @author rjsuzuki
 */
object GoogleResponse {

    private const val TAG = "BillingzGoogle"
    private const val BILLING_RESPONSE = "Billing Response"

    /**
     * Convenience logger to interpret the integer code of the [BillingResult].
     * - Also see [BillingClient.BillingResponseCode]
     * @param billingResult
     */
    fun logResult(billingResult: BillingResult?) {
        when (billingResult?.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                Logger.d(
                    TAG,
                    createLogMessage("OK", BillingClient.BillingResponseCode.OK, billingResult.debugMessage)
                )
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Logger.w(
                    TAG,
                    createLogMessage("USER_CANCELED", BillingClient.BillingResponseCode.USER_CANCELED, billingResult.debugMessage)
                )
            }
            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> {
                Logger.w(
                    TAG,
                    createLogMessage("BILLING_UNAVAILABLE", BillingClient.BillingResponseCode.BILLING_UNAVAILABLE, billingResult.debugMessage)
                )
            }
            BillingClient.BillingResponseCode.DEVELOPER_ERROR -> {
                Logger.e(
                    TAG,
                    createLogMessage("DEVELOPER_ERROR", BillingClient.BillingResponseCode.DEVELOPER_ERROR, billingResult.debugMessage)
                )
            }
            BillingClient.BillingResponseCode.ERROR -> {
                Logger.e(
                    TAG,
                    createLogMessage("ERROR", BillingClient.BillingResponseCode.ERROR, billingResult.debugMessage)
                )
            }
            BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED -> {
                Logger.w(
                    TAG,
                    createLogMessage("FEATURE_NOT_SUPPORTED", BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED, billingResult.debugMessage)
                )
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                Logger.w(
                    TAG,
                    createLogMessage("ITEM_ALREADY_OWNED", BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED, billingResult.debugMessage)
                )
            }
            BillingClient.BillingResponseCode.ITEM_NOT_OWNED -> {
                Logger.w(
                    TAG,
                    createLogMessage("ITEM_NOT_OWNED", BillingClient.BillingResponseCode.ITEM_NOT_OWNED, billingResult.debugMessage)
                )
            }
            BillingClient.BillingResponseCode.ITEM_UNAVAILABLE -> {
                Logger.w(
                    TAG,
                    createLogMessage("ITEM_UNAVAILABLE", BillingClient.BillingResponseCode.ITEM_UNAVAILABLE, billingResult.debugMessage)
                )
            }
            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED -> {
                Logger.w(
                    TAG,
                    createLogMessage("SERVICE_DISCONNECTED", BillingClient.BillingResponseCode.SERVICE_DISCONNECTED, billingResult.debugMessage)
                )
            }
            BillingClient.BillingResponseCode.SERVICE_TIMEOUT -> {
                Logger.e(
                    TAG,
                    createLogMessage("SERVICE_TIMEOUT", BillingClient.BillingResponseCode.SERVICE_TIMEOUT, billingResult.debugMessage)
                )
            }
            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE -> {
                Logger.e(
                    TAG,
                    createLogMessage("SERVICE_UNAVAILABLE", BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE, billingResult.debugMessage)
                )
            }
            BillingClient.BillingResponseCode.NETWORK_ERROR -> {
                Logger.e(
                    TAG,
                    createLogMessage("NETWORK_ERROR", BillingClient.BillingResponseCode.NETWORK_ERROR, billingResult.debugMessage)
                )
            }
            else -> {
                Logger.wtf(
                    TAG,
                    "Unhandled $BILLING_RESPONSE: ${billingResult?.responseCode}"
                )
            }
        }
    }

    private fun createLogMessage(
        result: String,
        billingResponseCode: Int,
        message: String
    ): String {
        val msg = "$BILLING_RESPONSE:" +
            "\n result: $result" +
            "\n code: $billingResponseCode," +
            "\n message: $message"
        return msg
    }
}
