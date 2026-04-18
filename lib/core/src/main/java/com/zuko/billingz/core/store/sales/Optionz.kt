package com.zuko.billingz.core.store.sales

import android.os.Bundle

object Optionz {

    enum class Type {
        PRORATION_MODE,
        ORIGINAL_EXTERNAL_TRANSACTION_ID,
        OLD_PURCHASE_TOKEN,
        IS_PERSONALIZED_OFFER,
        SELECTED_OFFER_INDEX
    }

    /**
     * Builder for setting customizable options, such as modifying an existing subscription or
     * updating pricing information for a purchase flow.
     * To modify an active subscription, make sure to set the desired proration mode, the purchase token, and the productId
     * of the relevant subscription.
     */
    class Builder : OptionBuilder {
        private var isOfferPersonalized = false
        private var prorationMode: Int = -1
        private var oldPurchaseToken: String? = null
        private var originalExternalTransactionId: String? = null
        private var selectedOfferIndex = -1

        override fun setIsOfferPersonalized(isOfferPersonalized: Boolean): Builder {
            this.isOfferPersonalized = isOfferPersonalized
            return this
        }

        override fun setProrationMode(mode: Int): Builder {
            prorationMode = mode
            return this
        }

        override fun setOldPurchaseToken(token: String): Builder {
            oldPurchaseToken = token
            return this
        }

        override fun setOriginalExternalTransactionId(id: String): Builder {
            originalExternalTransactionId = id
            return this
        }

        override fun setSelectedOfferIndex(index: Int): Builder {
            selectedOfferIndex = index
            return this
        }

        override fun build(): Bundle {
            val bundle = Bundle()
            bundle.putString(Type.ORIGINAL_EXTERNAL_TRANSACTION_ID.name, originalExternalTransactionId)
            bundle.putString(Type.OLD_PURCHASE_TOKEN.name, oldPurchaseToken)
            bundle.putInt(Type.PRORATION_MODE.name, prorationMode)
            bundle.putBoolean(Type.IS_PERSONALIZED_OFFER.name, isOfferPersonalized)
            bundle.putInt(Type.SELECTED_OFFER_INDEX.name, selectedOfferIndex)
            return bundle
        }
    }

    sealed interface OptionBuilder {

        /**
         * Only for Google Play consumers in the European Union.
         * When true, the Play UI includes the disclosure. When false, the UI omits the disclosure.
         * The default value is false.
         * You must consult Art. 6 (1) (ea) [CRD of the Consumer Rights Directive](https://eur-lex.europa.eu/legal-content/EN/TXT/?uri=CELEX:02011L0083-20220528) (2011/83/EU)
         * to determine if the price you are offering to users is personalized.
         */
        fun setIsOfferPersonalized(isOfferPersonalized: Boolean): Builder

        /**
         * Modify the proration settings for subscriptions.
         * - Refer to [Google's BillingFlowParams.ProrationMode](https://developer.android.com/reference/com/android/billingclient/api/BillingFlowParams.ProrationMode)
         */
        fun setProrationMode(mode: Int): Builder

        /**
         * Set the purchase token for the subscription to be modified.
         */
        fun setOldPurchaseToken(token: String): Builder

        /**
         * Set the [original external transaction id](https://developer.android.com/reference/com/android/billingclient/api/BillingFlowParams.SubscriptionUpdateParams.Builder#setOriginalExternalTransactionId(java.lang.String)) of the subscription to be modified.
         */
        fun setOriginalExternalTransactionId(id: String): Builder

        /**
         * Set the index (position) of the relevant Subscription OfferDetails to
         * be purchased.
         */
        fun setSelectedOfferIndex(index: Int): Builder

        /**
         * Create [Bundle] object of order options.
         */
        fun build(): Bundle
    }
}
