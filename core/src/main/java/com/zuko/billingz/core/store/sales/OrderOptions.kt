package com.zuko.billingz.core.store.sales

import android.os.Bundle

object OrderOptions {
    const val IS_SUB_CHANGE_KEY = "IS_SUB_CHANGE"
    const val PRORATION_MODE_KEY = "PRORATION_MODE_KEY"
    const val OLD_SUB_SKU_KEY = "OLD_SUB_SKU"
    const val OLD_PURCHASE_TOKEN_KEY = "OLD_PURCHASE_TOKEN_KEY"

    class Builder {
        private var prorationMode: Int = 0
        private var oldSubSku: String? = null

        fun setProrationMode(mode: Int): Builder {
            prorationMode = mode
            return this
        }

        fun setOldSubscriptionSku(id: String): Builder {
            oldSubSku = id
            return this
        }

        fun build(): Bundle {
            val bundle = Bundle()
            bundle.putBoolean(IS_SUB_CHANGE_KEY, true)
            bundle.putString(OLD_SUB_SKU_KEY, oldSubSku)
            bundle.putInt(PRORATION_MODE_KEY, prorationMode)
            return bundle
        }
    }
}