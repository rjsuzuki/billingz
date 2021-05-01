package com.zuko.billingz.lib.misc

import androidx.annotation.UiThread

interface CleanUp {

    @UiThread
    fun destroy()
}
