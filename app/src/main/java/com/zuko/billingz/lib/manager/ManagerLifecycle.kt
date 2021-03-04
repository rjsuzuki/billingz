package com.zuko.billingz.lib.manager

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.OnLifecycleEvent
import com.zuko.billingz.lib.sales.Sales

interface ManagerLifecycle {
    fun init(context: Context?)

    /**
     * Initiate logic dependent on Android's onCreate() Lifecycle event.
     * If you added the Manager class as a lifecycleObserver, you do
     * not need to add this class manually in your activity/fragment.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun create()

    /**
     * Initiate logic dependent on Android's onStart() Lifecycle event.
     * If you added the Manager class as a lifecycleObserver, you do
     * not need to add this class manually in your activity/fragment.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun start()

    /**
     * Initiate logic dependent on Android's onResume() Lifecycle event.
     * If you added the Manager class as a lifecycleObserver, you do
     * not need to add this class manually in your activity/fragment.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun resume()

    /**
     * Initiate logic dependent on Android's onPause() Lifecycle event.
     * If you added the Manager class as a lifecycleObserver, you do
     * not need to add this class manually in your activity/fragment.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun pause()

    /**
     * Initiate logic dependent on Android's onStop() Lifecycle event.
     * If you added the Manager class as a lifecycleObserver, you do
     * not need to add this class manually in your activity/fragment.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun stop()

    /**
     * Initiate logic dependent on Android's onDestroy() Lifecycle event.
     * If you added the Manager class as a lifecycleObserver, you do
     * not need to add this class manually in your activity/fragment.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun destroy()
}