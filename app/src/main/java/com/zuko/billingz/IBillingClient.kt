package com.zuko.billingz

import android.app.Activity
import androidx.lifecycle.MutableLiveData
import com.android.billingclient.api.SkuDetails
import com.zuko.billingz.lib.model.PurchaseWrapper

interface IBillingClient {

    val isReadyLiveData: MutableLiveData<Boolean>
    val purchaseLiveData: MutableLiveData<PurchaseWrapper>
    fun purchase(activity: Activity?, productId: String?)

    fun getProductDetails(productId: String?) : SkuDetails?


    fun loadSubscriptionProducts(skuList: MutableList<String>)
    fun loadInAppProducts(skuList: MutableList<String>, isConsumables: Boolean)

    fun enableDeveloperMode(mockList: MutableList<String>, type: BillingManager.ProductType)
    fun disableDeveloperMode()
}