package com.zuko.billingz.lib.facade

import android.app.Activity
import androidx.lifecycle.LiveData
import com.android.billingclient.api.SkuDetails
import com.zuko.billingz.lib.sales.Order

/**
 * @author rjsuzuki
 */
interface DevControl {

    /**
     * @return LiveData
     */
    fun purchase(activity: Activity?, productId: String?) : LiveData<Order>


    //fun purchase(activity: Activity?, productId: String?)

    fun getProductDetails(productId: String?) : SkuDetails?


    fun enableDeveloperMode()
}