package com.zuko.billingz.lib

import android.util.Log

object LogUtil {

    private var verbosity: Int = Log.DEBUG

    /**
     * Public method
     */
    fun setLogLevel(level: Int) {

    }

    private var isWarningsEnabled = true
    private var isErrorsEnabled = true

    /**
     * A way to forward the library's logs to the integrated
     * app so that developers can merge the logs into their
     * own logging system like Crashlytics.
     */
    private var isLogForwardingEnabled = false


    object log {

        //2
        fun v(tag: String, msg: String) {
            if(verbosity >= Log.VERBOSE)
                Log.v(tag, msg)
        }

        //3
        fun d(tag: String, msg: String) {
            if(verbosity >= Log.DEBUG)
                Log.d(tag, msg)
        }

        //4
        fun i(tag: String, msg: String) {
            if(verbosity >= Log.INFO)
                Log.i(tag, msg)
        }

        //5
        fun w(tag: String, msg: String) {
            if(isWarningsEnabled)
                 Log.w(tag, msg)
        }
        //6
        fun e(tag: String, msg: String) {
            if(isErrorsEnabled)
                 Log.e(tag, msg)
        }

        //7
        fun wtf(tag: String, msg: String) {
            Log.wtf(tag, msg)
        }
    }
}