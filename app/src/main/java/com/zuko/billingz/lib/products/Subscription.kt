package com.zuko.billingz.lib.products

import com.android.billingclient.api.Purchase
import com.android.billingclient.api.SkuDetails

interface Subscription: Product  {

    //sub state
    fun getState()
}