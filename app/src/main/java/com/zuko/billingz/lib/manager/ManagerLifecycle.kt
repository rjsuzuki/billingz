package com.zuko.billingz.lib.manager

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.OnLifecycleEvent

interface ManagerLifecycle {
    fun init(context: Context?)

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun create()

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun resume()

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun pause()

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun destroy()
}