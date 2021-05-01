package com.zuko.billingz.lib.misc

import android.util.Log
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.zuko.billingz.lib.LogUtil

/**
 * @author rjsuzuki
 */
object BillingResponse {

    private const val TAG = "BillingResponse"

    fun logResult(billingResult: BillingResult?) {
        when (billingResult?.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                LogUtil.log.d(TAG, BillingClient.BillingResponseCode.OK.toString())
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                LogUtil.log.w(TAG, BillingClient.BillingResponseCode.USER_CANCELED.toString())
            }
            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> {
                LogUtil.log.w(TAG, BillingClient.BillingResponseCode.BILLING_UNAVAILABLE.toString())
            }
            BillingClient.BillingResponseCode.DEVELOPER_ERROR -> {
                LogUtil.log.e(TAG, BillingClient.BillingResponseCode.DEVELOPER_ERROR.toString())
            }
            BillingClient.BillingResponseCode.ERROR -> {
                LogUtil.log.e(TAG, BillingClient.BillingResponseCode.ERROR.toString())
            }
            BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED -> {
                LogUtil.log.w(TAG, BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED.toString())
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                LogUtil.log.w(TAG, BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED.toString())
            }
            BillingClient.BillingResponseCode.ITEM_NOT_OWNED -> {
                LogUtil.log.w(TAG, BillingClient.BillingResponseCode.ITEM_NOT_OWNED.toString())
            }
            BillingClient.BillingResponseCode.ITEM_UNAVAILABLE -> {
                LogUtil.log.w(TAG, BillingClient.BillingResponseCode.ITEM_UNAVAILABLE.toString())
            }
            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED -> {
                LogUtil.log.w(TAG, BillingClient.BillingResponseCode.SERVICE_DISCONNECTED.toString())
            }
            BillingClient.BillingResponseCode.SERVICE_TIMEOUT -> {
                LogUtil.log.e(TAG, BillingClient.BillingResponseCode.SERVICE_TIMEOUT.toString())
            }
            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE -> {
                LogUtil.log.e(TAG, BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE.toString())
            }
            else -> {
                Log.w(TAG, "Unhandled response code: ${billingResult?.responseCode}")
            }
        }
    }
}
