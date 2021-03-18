package com.zuko.billingz.lib.extra

import android.util.Log
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult


/**
 * @author rjsuzuki
 */
object BillingResponseCodes {

    private const val TAG = "BillingResponseCodes"

    private fun handlePurchaseError(billingResult: BillingResult?) {
        when(billingResult?.responseCode) {
            BillingClient.BillingResponseCode.USER_CANCELED -> {

            }
            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> {

            }
            BillingClient.BillingResponseCode.DEVELOPER_ERROR -> {

            }
            BillingClient.BillingResponseCode.ERROR -> {

            }
            BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED -> {

            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {

            }
            BillingClient.BillingResponseCode.ITEM_NOT_OWNED -> {

            }
            BillingClient.BillingResponseCode.ITEM_UNAVAILABLE -> {

            }
            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED -> {

            }
            BillingClient.BillingResponseCode.SERVICE_TIMEOUT -> {

            }
            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE -> {

            }
            else -> {
                Log.w(TAG, "Unhandled response code: ${billingResult?.responseCode}")
            }
        }
    }

}