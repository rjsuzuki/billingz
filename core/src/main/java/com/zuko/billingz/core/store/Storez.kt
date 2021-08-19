package com.zuko.billingz.core.store

import com.zuko.billingz.core.store.agent.Agentz

interface Storez : StoreLifecycle {

    /**
     * Returns the primary class for developers to conveniently
     * interact with one of the supported billing libraries, such as Android's Billing Library.
     * (Facade pattern)
     * @return [Agentz]
     */
    fun getAgent(): Agentz
}
