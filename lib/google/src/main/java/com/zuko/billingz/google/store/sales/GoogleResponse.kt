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
                    "$BILLING_RESPONSE: " + "\n " + BillingClient.BillingResponseCode.OK.toString() + "\n message: OK"
                )
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Logger.w(
                    TAG,
                    "$BILLING_RESPONSE: " + "\n code: " + BillingClient.BillingResponseCode.USER_CANCELED.toString() + "\n message: ${billingResult.debugMessage}"
                )
            }
            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> {
                Logger.w(
                    TAG,
                    "$BILLING_RESPONSE: " + "\n code: " + BillingClient.BillingResponseCode.BILLING_UNAVAILABLE.toString() + "\n message: ${billingResult.debugMessage}"
                )
            }
            BillingClient.BillingResponseCode.DEVELOPER_ERROR -> {
                Logger.e(
                    TAG,
                    "$BILLING_RESPONSE: " + "\n code: " + BillingClient.BillingResponseCode.DEVELOPER_ERROR.toString() + "\n message: ${billingResult.debugMessage}"
                )
            }
            BillingClient.BillingResponseCode.ERROR -> {
                Logger.e(
                    TAG,
                    "$BILLING_RESPONSE: " + "\n code: " + BillingClient.BillingResponseCode.ERROR.toString() + "\n message: ${billingResult.debugMessage}"
                )
            }
            BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED -> {
                Logger.w(
                    TAG,
                    "$BILLING_RESPONSE: " + "\n code: " + BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED.toString() + "\n message: ${billingResult.debugMessage}"
                )
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                Logger.w(
                    TAG,
                    "$BILLING_RESPONSE: " + "\n code: " + BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED.toString() + "\n message: ${billingResult.debugMessage}"
                )
            }
            BillingClient.BillingResponseCode.ITEM_NOT_OWNED -> {
                Logger.w(
                    TAG,
                    "$BILLING_RESPONSE: " + "\n code: " + BillingClient.BillingResponseCode.ITEM_NOT_OWNED.toString() + "\n message: ${billingResult.debugMessage}"
                )
            }
            BillingClient.BillingResponseCode.ITEM_UNAVAILABLE -> {
                Logger.w(
                    TAG,
                    "$BILLING_RESPONSE: " + "\n code: " + BillingClient.BillingResponseCode.ITEM_UNAVAILABLE.toString() + "\n message: ${billingResult.debugMessage}"
                )
            }
            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED -> {
                Logger.w(
                    TAG,
                    "$BILLING_RESPONSE: " + "\n code: " + BillingClient.BillingResponseCode.SERVICE_DISCONNECTED.toString() + "\n message: ${billingResult.debugMessage}"
                )
            }
            BillingClient.BillingResponseCode.SERVICE_TIMEOUT -> {
                Logger.e(
                    TAG,
                    "$BILLING_RESPONSE: " + "\n code: " + BillingClient.BillingResponseCode.SERVICE_TIMEOUT.toString() + "\n message: ${billingResult.debugMessage}"
                )
            }
            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE -> {
                Logger.e(
                    TAG,
                    "$BILLING_RESPONSE: " + "\n code: " + BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE.toString() + "\n message: ${billingResult.debugMessage}"
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
}
