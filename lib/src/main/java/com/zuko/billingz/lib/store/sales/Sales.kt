package com.zuko.billingz.lib.store.sales

import android.app.Activity
import androidx.lifecycle.MutableLiveData
import com.zuko.billingz.lib.misc.CleanUpListener
import com.zuko.billingz.lib.store.client.Client
import com.zuko.billingz.lib.store.model.Order
import com.zuko.billingz.lib.store.model.Product
import com.zuko.billingz.lib.store.model.Receipt

interface Sales: CleanUpListener {
    /**
     * Provides a liveData [Order] object for
     * developers to observe and react to on
     * the UI/Main thread.
     * Objects can be passed from the normal purchase flow
     * or when the app is verifying a list of queried purchases.
     */
    var currentReceipt: MutableLiveData<Receipt>

    /**
     *
     */
    var orderHistory: MutableLiveData<List<Receipt>>

    /**
     *
     */
    var orderUpdaterListener: OrderUpdaterListener?

    /**
     *
     */
    var orderValidatorListener: OrderValidatorListener?

    /**
     *
     */
    fun startOrder(activity: Activity?, product: Product, client: Client)

    /**
     *
     */
    fun validateOrder(order: Order)

    /**
     *
     */
    fun processOrder(order: Order)

    /**
     *
     */
    fun completeOrder(order: Order)

    /**
     *
     */
    fun cancelOrder(order: Order)

    /**
     *
     */
    fun failedOrder(order: Order)

    /**
     *
     */
    fun refreshQueries()

    /**
     *
     */
    fun queryOrders()

    /**
     *
     */
    fun queryReceipts(type: Product.Type? = null)

    /**
     * For developers to implement.
     * Enables developer to provide another verification step before finalizing an order. Also,
     * Purchases can be made outside of app, or finish while app is in background, and may not have
     * completed in a regular ui-flow and requires attention again.
     * show in-app popup, or deliver msg to an inbox, or use an OS notification.
     */
    interface OrderUpdaterListener {

        /**
         * @param order
         * @param productType
         * @param callback
         */
        fun onResume(order: Order, callback: UpdaterCallback)

        /**
         *
         */
        fun onComplete(receipt: Receipt)

        fun onError(order: Order)
    }

    /**
     *
     */
    interface UpdaterCallback {

        /**
         * Final step in completing an order. Developers should implement a way to persist their
         * Receipts prior to calling this method.
         */
        fun complete(order: Order)

        /**
         *
         */
        fun cancel(order: Order)
    }

    /**
     * For developers to implement.
     * Enables the ability to verify purchases with your own logic,
     * ensure entitlement was not already granted for this purchaseToken,
     * and grant entitlement to the user.
     */
    interface OrderValidatorListener {

        /**
         * @param order
         * @param callback
         */
        fun validate(order: Order, callback: ValidatorCallback)
    }

    /**
     * Respond to the events triggered by the developer's validator.
     * Developers will need to implement this interface if custom validation checks
     * need to be provided before finalizing an order.
     * If the purchase is properly verified, call onSuccess,
     * otherwise call onFailure so the library can appropriately continue the
     * lifecycle of a customer's order.
     */
    interface ValidatorCallback {

        /**
         * Developers should verify the order with their own backend records of a users purchase
         * history prior to calling this method.
         * @param order
         */
        fun validated(order: Order)

        /**
         * Call if order is deemed invalid due to the nature of the purchase. i.e. the order was
         * fulfilled already or the sku is no longer available, etc.
         * @param order
         */
        fun invalidate(order: Order)
    }
}