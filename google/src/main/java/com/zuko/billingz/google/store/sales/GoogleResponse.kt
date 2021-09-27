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

import android.util.Log
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.zuko.billingz.core.LogUtilz

/**
 * @author rjsuzuki
 */
object GoogleResponse {

    private const val TAG = "BillingResponsez"

    /**
     * Convenience logger to interpret the integer code of the [BillingResult]
     * @param billingResult
     */
    fun logResult(billingResult: BillingResult?) {
        when (billingResult?.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                LogUtilz.log.d(TAG, BillingClient.BillingResponseCode.OK.toString() + ": ${billingResult.debugMessage}")
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                LogUtilz.log.w(TAG, BillingClient.BillingResponseCode.USER_CANCELED.toString() + ": ${billingResult.debugMessage}")
            }
            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> {
                LogUtilz.log.w(TAG, BillingClient.BillingResponseCode.BILLING_UNAVAILABLE.toString() + ": ${billingResult.debugMessage}")
            }
            BillingClient.BillingResponseCode.DEVELOPER_ERROR -> {
                LogUtilz.log.e(TAG, BillingClient.BillingResponseCode.DEVELOPER_ERROR.toString() + ": ${billingResult.debugMessage}")
            }
            BillingClient.BillingResponseCode.ERROR -> {
                LogUtilz.log.e(TAG, BillingClient.BillingResponseCode.ERROR.toString() + ": ${billingResult.debugMessage}")
            }
            BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED -> {
                LogUtilz.log.w(TAG, BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED.toString() + ": ${billingResult.debugMessage}")
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                LogUtilz.log.w(TAG, BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED.toString() + ": ${billingResult.debugMessage}")
            }
            BillingClient.BillingResponseCode.ITEM_NOT_OWNED -> {
                LogUtilz.log.w(TAG, BillingClient.BillingResponseCode.ITEM_NOT_OWNED.toString() + ": ${billingResult.debugMessage}")
            }
            BillingClient.BillingResponseCode.ITEM_UNAVAILABLE -> {
                LogUtilz.log.w(TAG, BillingClient.BillingResponseCode.ITEM_UNAVAILABLE.toString() + ": ${billingResult.debugMessage}")
            }
            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED -> {
                LogUtilz.log.w(TAG, BillingClient.BillingResponseCode.SERVICE_DISCONNECTED.toString() + ": ${billingResult.debugMessage}")
            }
            BillingClient.BillingResponseCode.SERVICE_TIMEOUT -> {
                LogUtilz.log.e(TAG, BillingClient.BillingResponseCode.SERVICE_TIMEOUT.toString() + ": ${billingResult.debugMessage}")
            }
            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE -> {
                LogUtilz.log.e(TAG, BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE.toString() + ": ${billingResult.debugMessage}")
            }
            else -> {
                Log.wtf(TAG, "Unhandled response code: ${billingResult?.responseCode}")
            }
        }
    }
}
