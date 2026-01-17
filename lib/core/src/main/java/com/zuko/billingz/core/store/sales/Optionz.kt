package com.zuko.billingz.core.store.sales

import android.os.Bundle

object Optionz {

    enum class Type {
        PRORATION_MODE,
        OLD_SUB_ID,
        OLD_PURCHASE_TOKEN,
        IS_PERSONALIZED_OFFER,
        REGION,
        SELECTED_OFFER_INDEX,
        SELECTED_OFFER_ID
    }

    enum class Region {
        /** North America **/
        NA,

        /** Europe **/
        EU,

        /** Asia & Pacific Islands **/
        ASIA,

        /** South America **/
        SA,

        /** Africa **/
        AFR
    }

    /**
     * Builder for setting customizable options, such as modifying an existing subscription or
     * updating pricing information for a purchase flow.
     * To modify an active subscription, make sure to set the desired proration mode, the purchase token, and the productId
     * of the relevant subscription.
     */
    class Builder : OptionBuilder {
        private var region: Region = Region.NA
        private var isOfferPersonalized = false
        private var prorationMode: Int = -1
        private var oldPurchaseToken: String? = null
        private var oldSubId: String? = null
        private var selectedOfferIndex = -1
        private var selectedOfferId: String? = null

        override fun setConsumerRegion(region: Region): Builder {
            this.region = region
            return this
        }

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

        override fun setOldSubscriptionId(id: String): Builder {
            oldSubId = id
            return this
        }

        override fun setSelectedOfferIndex(index: Int): Builder {
            selectedOfferIndex = index
            return this
        }

        override fun setSelectedOfferId(offerId: String): Builder {
            selectedOfferId = offerId
            return this
        }

        override fun build(): Bundle {
            val bundle = Bundle()
            bundle.putString(Type.OLD_SUB_ID.name, oldSubId)
            bundle.putString(Type.OLD_PURCHASE_TOKEN.name, oldPurchaseToken)
            bundle.putInt(Type.PRORATION_MODE.name, prorationMode)
            if (region != Region.EU) {
                isOfferPersonalized = false
            }
            bundle.putInt(Type.REGION.name, region.ordinal)
            bundle.putBoolean(Type.IS_PERSONALIZED_OFFER.name, isOfferPersonalized)
            bundle.putInt(Type.SELECTED_OFFER_INDEX.name, selectedOfferIndex)
            bundle.putString(Type.SELECTED_OFFER_ID.name, selectedOfferId)
            return bundle
        }
    }

    sealed interface OptionBuilder {
        /**
         * Set the consumer's region of origin for region-specific
         * customization options.
         * Default region is [Region.NA].
         */
        fun setConsumerRegion(region: Region): Builder

        /**
         * Only for Google Play consumers in the European Union.
         * When true, the Play UI includes the disclosure. When false, the UI omits the disclosure.
         * The default value is false. Or, if [setConsumerRegion] is not updated to the [Region.EU], this value will default to false.
         * You must consult Art. 6 (1) (ea) CRD of the Consumer Rights Directive (2011/83/EU)
         * [https://eur-lex.europa.eu/legal-content/EN/TXT/?uri=CELEX:02011L0083-20220528]
         * to determine if the price you are offering to users is personalized.
         *
         */
        fun setIsOfferPersonalized(isOfferPersonalized: Boolean): Builder

        /**
         * Modify the proration settings for subscriptions.
         * Refer to Google's BillingFlowParams.ProrationMode [https://developer.android.com/reference/com/android/billingclient/api/BillingFlowParams.ProrationMode]
         */
        fun setProrationMode(mode: Int): Builder

        /**
         * Set the purchase token for the subscription to be modified.
         */
        fun setOldPurchaseToken(token: String): Builder

        /**
         * Set the product id of the subscription to be modified.
         */
        fun setOldSubscriptionId(id: String): Builder

        /**
         * Set the index (position) of the relevant Subscription OfferDetails to
         * be purchased.
         */
        fun setSelectedOfferIndex(index: Int): Builder

        /**
         * Set the offer id of the relevant Subscription OfferDetails to
         * be purchased.
         */
        fun setSelectedOfferId(offerId: String): Builder

        /**
         * Create [Bundle] object of order options.
         */
        fun build(): Bundle
    }
}
