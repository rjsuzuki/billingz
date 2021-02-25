package com.zuko.billingz.lib.client

import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PurchasesUpdatedListener
import com.zuko.billingz.lib.model.PurchaseWrapper
import com.zuko.billingz.lib.model.Result
import com.zuko.billingz.lib.sales.Order

interface Billing {

    var order: MutableLiveData<Order>

    fun getBillingClient(): BillingClient?
    fun isReady(): Boolean
    fun initClient(context: Context?, listener: PurchasesUpdatedListener)

    fun connect()
    fun disconnect()

    fun error(billingResult: BillingResult?)
}