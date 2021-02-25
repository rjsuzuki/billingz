package com.zuko.billingz.lib.products

import com.android.billingclient.api.Purchase
import com.android.billingclient.api.SkuDetails

interface Consumable : Product {

    var consumables: Map<String, SkuDetails>

    /**
     * For consumables, the consumeAsync() method fulfills the acknowledgement requirement and
     * indicates that your app has granted entitlement to the user. This method also enables your app
     * to make the one-time product available for purchase again.
     * To indicate that a one-time product has been consumed, call consumeAsync() and include the
     * purchase token that Google Play should make available for repurchase. You must also pass an
     * object that implements the ConsumeResponseListener interface. This object handles the result
     * of the consumption operation. You can override the onConsumeResponse() method,
     * which the Google Play Billing Library calls when the operation is complete.
     */
    fun processConsumable(purchase: Purchase)

    fun loadConsumableProducts(skuList: MutableList<String>)
    //findConsumableById
    //get Consumable details
}