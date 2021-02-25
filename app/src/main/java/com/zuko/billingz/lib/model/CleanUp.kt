package com.zuko.billingz.lib.model

import androidx.annotation.UiThread

interface CleanUp {

    @UiThread
    fun destroy()
}