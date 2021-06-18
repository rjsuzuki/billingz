package com.zuko.billingz.google.store.client

import com.android.billingclient.api.BillingClient
import com.zuko.billingz.lib.store.client.Client

interface GoogleClient: Client {

    fun getBillingClient(): BillingClient?
}