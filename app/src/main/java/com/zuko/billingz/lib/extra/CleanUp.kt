package com.zuko.billingz.lib.extra

import androidx.annotation.UiThread

interface CleanUp {

    @UiThread
    fun destroy()
}