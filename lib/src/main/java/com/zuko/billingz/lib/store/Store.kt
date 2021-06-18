package com.zuko.billingz.lib.store

import com.zuko.billingz.lib.store.agent.Agent

interface Store : StoreLifecycle {

    /**
     * Returns the primary class for developers to conveniently
     * interact with one of the supported billing libraries, such as Android's Billing Library.
     * (Facade pattern)
     * @return [Agent]
     */
    fun getAgent(): Agent
}
