package com.zuko.billingz

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.*
import com.android.billingclient.api.*
import com.zuko.billingz.lib.model.PurchaseWrapper

import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

open class BillingManager constructor(open val context: Context) : LifecycleObserver, PurchasesUpdatedListener, IBillingManager, IBillingClient,
    CoroutineScope {

    enum class ProductType { CONSUMABLE, NON_CONSUMABLE, SUBSCRIPTION }

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    // For testing only
    private var isDeveloperMode = false
    private var mockList = mutableListOf<String>()
    private var mockType = ProductType.CONSUMABLE

    // Track all available products from Google Play
    private var allProducts: Map<String, SkuDetails> = HashMap() //all products

    private var subscriptions: Map<String, SkuDetails> = HashMap()
    private var consumables: Map<String, SkuDetails> = HashMap()
    private var nonConsumables: Map<String, SkuDetails> = HashMap()

    private var ownedSubscriptions: MutableList<Purchase>? = null
    private var ownedInappProducts: MutableList<Purchase>? = null

    //Observables/LiveData
    val skuDetailsLiveData = MutableLiveData<HashMap<String, SkuDetails>>()

    /**
     * Observe updates to purchases
     */
    override val purchaseLiveData = MutableLiveData<PurchaseWrapper>()

    /**
     * Observe changes between when the Client is
     * successfully connected or not
     */
    override val isReadyLiveData = MutableLiveData<Boolean>()

    private var billingClient: BillingClient? = null
    private var isConnected = false

    private var purchase: Purchase? = null
    private var purchaseToken: String? = null
    private var orderId: String? = null

    private var isAlreadyQueried = false

    /**
     * @return Boolean
     * Checks if the client both initialized and is currently connected to the service,
     * so that requests to other methods will succeed.
     */
    fun isReady(): Boolean {
        return isConnected && billingClient?.isReady == true
    }

    /*****************************************************************************************************
     * Lifecycle events - developer must either add this class to a lifecycleOwner or manually add the events
     * to their respective parent view
     *****************************************************************************************************/

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun onCreate() {
        initClient(context)
        connect()
        refreshPurchaseHistory(true)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onResume() {
        refreshPurchaseHistory(false)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onPause() {
        //todo
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        billingClient?.endConnection()
        billingClient = null
        cancel()
    }

    /*****************************************************************************************************
     * Initialization
     *****************************************************************************************************/

    private fun initClient(context: Context) {
        if(billingClient != null) {
            billingClient?.endConnection() //will this throw an error?
            billingClient = null
        }
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases() //switch
            .build()
    }

    private fun connect() {
        billingClient?.startConnection(object: BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                when(billingResult.responseCode) {
                    BillingClient.BillingResponseCode.OK -> {
                        // The BillingClient is ready. You can query purchases here.
                        isConnected = true
                        isReadyLiveData.postValue(true)
                        initMockData()
                    }
                    else -> {
                        Log.w(TAG, "Unhandled response code: ${billingResult.responseCode}")
                        isConnected = false
                    }
                }
            }

            override fun onBillingServiceDisconnected() {
                isConnected = false
                isReadyLiveData.postValue(false)
                // Note: It's strongly recommended that you implement your own connection retry logic
                // and override the onBillingServiceDisconnected() method.
                // Make sure you maintain the BillingClient connection when executing any methods.
            }
        })
    }

    /**********************************************************************
     * Fetching the available products from Google Play
     *********************************************************************/

    override fun enableDeveloperMode(mockList: MutableList<String>, type: ProductType) {
        isDeveloperMode = true
        this.mockList = mockList
        this.mockType = type
        Log.d(TAG, "Is Developer Mode active? ${isDeveloperMode}")
    }

    override fun disableDeveloperMode() {
        isDeveloperMode = false
    }

    private fun initMockData() {
        if(isDeveloperMode) {
            querySkuDetails(mockList, mockType)
            //todo mock SKu List and network calls instead
        }
    }

    /**
     * Get details of a specific product
     */
    override fun getProductDetails(productId: String?) : SkuDetails? {
        productId?.let {
            return allProducts[productId]
        } ?: return null
    }

    /**
     * @param isConsumables - indicate whether the skuList is for consumables or not. (Do not mix consumables
     * and non-consumables in same list if possible)
     * @param skuList, a list of string productIds that will try to match
     * against Google Play's list of available subscriptions
     */
    override fun loadInAppProducts(skuList: MutableList<String>, isConsumables: Boolean) {
        val type = if(isConsumables) ProductType.CONSUMABLE else ProductType.NON_CONSUMABLE
        querySkuDetails(skuList, type)
    }

    /**
     * @param skuList, a list of string productIds that will try to match
     * against Google Play's list of available subscriptions
     */
    override fun loadSubscriptionProducts(skuList: MutableList<String>) {
        querySkuDetails(skuList, ProductType.SUBSCRIPTION)
    }

    private fun querySkuDetails(skuList: MutableList<String>, type: ProductType) {
        Log.v(TAG, "Sku details of type : $type with list of ${skuList.size}")
        val params = SkuDetailsParams.newBuilder()
        val skuType = if(type == ProductType.SUBSCRIPTION) BillingClient.SkuType.SUBS else BillingClient.SkuType.INAPP
        params.setSkusList(skuList).setType(skuType)
        launch(Dispatchers.IO) {
            billingClient?.querySkuDetailsAsync(params.build()) { result, skuDetailsList ->
                // process the result.
                Log.i(TAG, "query result : ${result.responseCode}, ${result.debugMessage}")
                updateSkuDetails(skuDetailsList, type)
            }
        }
    }

    @Synchronized
    private fun updateSkuDetails(skuDetailsList: List<SkuDetails>?, type: ProductType) {
        Log.d(TAG, "updateSkuDetails : ${skuDetailsList?.size ?: 0}")
        if (!skuDetailsList.isNullOrEmpty()) {
            allProducts = allProducts + skuDetailsList.associateBy { it.sku }

            when(type) {
                ProductType.CONSUMABLE -> {
                    consumables = consumables + skuDetailsList.associateBy { it.sku }
                }
                ProductType.NON_CONSUMABLE -> {
                    nonConsumables = nonConsumables + skuDetailsList.associateBy { it.sku }
                }
                ProductType.SUBSCRIPTION -> {
                    subscriptions = subscriptions + skuDetailsList.associateBy { it.sku }
                }
            }
        }
    }

    /**********************************************************************
     * Obtain purchase history records - Products currently owned by user
     *********************************************************************/

    private fun queryPurchaseHistory() {
        if(isReady()) {
            val listener = PurchaseHistoryResponseListener { billingResult, historyRecords ->
                TODO("Not yet implemented")
            }
            billingClient?.queryPurchaseHistoryAsync(BillingClient.SkuType.INAPP, listener)
        }
    }

    /**
     * Fetch all purchases to keep history up-to-date.
     * Network issues, multiple devices, and external purchases
     * could create untracked purchases - call this method in the
     * onCreate and onResume lifecycle events.
     */
    private fun refreshPurchaseHistory(isOnCreateEvent: Boolean) {
        if(isOnCreateEvent) {
            queryPurchases()
            isAlreadyQueried = true
        } else if(isAlreadyQueried) {
            // skip - prevents double queries on initialization
            isAlreadyQueried = false
        } else {
            queryPurchases()
        }
    }

    private fun queryPurchases() {
        if(isReady()) {
            val inappResult = billingClient?.queryPurchases(BillingClient.SkuType.INAPP)
            if(inappResult?.responseCode == BillingClient.BillingResponseCode.OK) {
                ownedInappProducts = inappResult.purchasesList
            }
            val subsResult =  billingClient?.queryPurchases(BillingClient.SkuType.SUBS)
            if(subsResult?.responseCode == BillingClient.BillingResponseCode.OK) {
                ownedSubscriptions = subsResult.purchasesList
            }
        }
    }

    /**********************************************************************
     * Purchase Flow
     *********************************************************************/

    override fun purchase(activity: Activity?, productId: String?) {
        if(activity == null || productId.isNullOrBlank() || !isReady()) {
            //todo
            Log.e(TAG, "Cannot start purchase flow")
        } else {
            startPurchaseRequest(activity, productId)
        }
    }

    private fun startPurchaseRequest(activity: Activity, id: String) : LiveData<BillingResult> {
        Log.d(TAG, "Starting purchase flow")
        val data = MutableLiveData<BillingResult>()

        val skuDetails = allProducts[id]

        // Retrieve a value for "skuDetails" by calling querySkuDetailsAsync().
        val flowParams = skuDetails?.let { flowParams ->
            BillingFlowParams.newBuilder()
                .setSkuDetails(flowParams)
                .build() // UI flow will start
        }
        flowParams?.let { flowParams2 ->
            val result = billingClient?.launchBillingFlow(activity, flowParams2)
            result?.let {
                Log.d(TAG, "Purchased flow finished : $it")
                data.postValue(result)
            }
        }
        return data
    }

    /**
     * Receives the result of the most recent purchase operation
     */
    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        Log.d(TAG, "onPurchasesUpdated")
        when(billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> handleUpdatedPurchases(purchases)
            else -> handlePurchaseError(billingResult)
        }
    }

    /**
     * The user has completed a purchase and the client has received a successful response
     */
    private fun handleUpdatedPurchases(purchases: MutableList<Purchase>?) {
        if(purchases.isNullOrEmpty()) {
            Log.d(TAG, "No purchases available")
        } else {
            launch(Dispatchers.IO) {
                for(p in purchases) {
                    when(p.purchaseState) {
                        Purchase.PurchaseState.PURCHASED -> handlePurchase(p)
                        Purchase.PurchaseState.PENDING -> handlePendingTransaction(p)
                        Purchase.PurchaseState.UNSPECIFIED_STATE -> handlePurchaseError(null)
                    }
                }
            }
        }
    }

    private fun isPurchaseVerified() : Boolean {
        //todo - get Purchase retrieved from BillingClient - queryPurchases
        // Verify the purchase.
        // Ensure entitlement was not already granted for this purchaseToken.
        // Grant entitlement to the user.
        return true
    }

    private fun processInAppPurchase(purchase: Purchase) {
        if(!consumables.isNullOrEmpty() && consumables.contains(purchase.sku)) {
            processConsumable(purchase)
        } else {
            processNonConsumable(purchase)
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if(isPurchaseVerified()) { //possibly move this into respective methods below

            //is inApp or Sub?
            val type = allProducts[purchase.sku]?.type

            if(type?.equals(BillingClient.SkuType.INAPP, ignoreCase = true) == true) {
                processInAppPurchase(purchase)
            }

            if(type?.equals(BillingClient.SkuType.SUBS, ignoreCase = true) == true) {
                processSubscription(purchase)
            }
        } else {
            //todo add error handling?
        }
    }

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
    override fun processConsumable(purchase: Purchase) {

        val consumeParams = ConsumeParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient?.consumeAsync(consumeParams) { billingResult, _ ->
            val msg: String
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                msg = "Consumable successfully purchased"
                Log.d(TAG, "Product successfully purchased: ${purchase.sku}")
                Toast.makeText(context, "successfully bought $purchase", Toast.LENGTH_LONG).show()
            } else {
                msg = billingResult.debugMessage
                handlePurchaseError(billingResult)
            }

            val data = PurchaseWrapper(purchase = purchase,
                billingResult = billingResult,
                msg = msg) // todo - consider adding enum state?
            purchaseLiveData.postValue(data)
        }
    }

    /**
     * To acknowledge non-consumable purchases, use either BillingClient.acknowledgePurchase()
     * from the Billing Library or Product.Purchases.Acknowledge from the Google Play Developer API.
     * Before acknowledging a purchase, your app should check whether it was already acknowledged by using
     * the isAcknowledged() method in the Google Play Billing Library or the acknowledgementState
     * field in the Google Developer API.
     */
    override fun processNonConsumable(purchase: Purchase) {

        val listener = AcknowledgePurchaseResponseListener { billingResult ->
            val data = PurchaseWrapper(purchase = purchase,
                billingResult = billingResult,
                msg = "Non-Consumable successfully purchased") // todo - consider adding enum state?
            purchaseLiveData.postValue(data)
        }

        if(purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if(!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken)
                val acknowledgePurchaseResult = launch(Dispatchers.IO) {
                    billingClient?.acknowledgePurchase(acknowledgePurchaseParams.build(), listener)
                }
            }
        }
    }

    /**
     * Subscriptions are handled similarly to non-consumables.
     * You can acknowledge a subscription Acknowledgement using either BillingClient.acknowledgePurchase()
     * from the Google Play Billing Library or Purchases.Subscriptions.Acknowledge from the Google Play Developer API.
     * All initial subscription purchases need to be acknowledged. Subscription renewals do not need to be
     * acknowledged. For more information on when subscriptions need to be acknowledged,
     * see the Sell subscriptions topic.
     */
    override fun processSubscription(purchase: Purchase) {

        val listener = AcknowledgePurchaseResponseListener { billingResult ->
            val data = PurchaseWrapper(purchase = purchase,
                billingResult = billingResult,
                msg = "Subscription successfully purchased") // todo - consider adding enum state?
            purchaseLiveData.postValue(data)
        }

        if(purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if(!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken)
                val acknowledgePurchaseResult = launch(Dispatchers.IO) {
                    billingClient?.acknowledgePurchase(acknowledgePurchaseParams.build(), listener)
                }
            }
        }
    }

    private fun handlePendingTransaction(purchase: Purchase) {
        throw NotImplementedError() //todo
    }

    /**
     * Mainly for debugging and logging
     */
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

    companion object {
        private const val TAG = "BillingManager2"
    }
}