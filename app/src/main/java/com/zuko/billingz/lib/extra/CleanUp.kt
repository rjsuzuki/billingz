package com.zuko.billingz.lib.extra

import androidx.annotation.UiThread

/**
 * Class for implementing resource clean up logic
 * @author rjsuzuki
 */
interface CleanUp {

    @UiThread
    fun destroy()
}