package com.zuko.billingz.lib.model

import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase

/**
 * Data Class (DTO) for wrapping a BillingClient's Purchase object
 * with other relevant data.
 * If developer needs to track more custom data,
 * this class can be extended
 */
data class PurchaseWrapper(
    var purchase: Purchase? = null,
    var isProcessing: Boolean = false,
    var billingResult: BillingResult? = null,
    var msg: String? = ""
)
