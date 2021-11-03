/*
 * Copyright 2021 rjsuzuki
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.zuko.billingz.core

import android.util.Log

object LogUtilz {

    private var verbosity: Int = Log.DEBUG

    /**
     * Public method
     */
    fun setLogLevel(level: Int) {
        if (level in 2..7) {
            verbosity = level
        }
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

        // 2
        fun v(tag: String, msg: String) {
            if (verbosity >= Log.VERBOSE)
                Log.v(tag, msg)
        }

        // 3
        fun d(tag: String, msg: String) {
            if (verbosity >= Log.DEBUG)
                Log.d(tag, msg)
        }

        // 4
        fun i(tag: String, msg: String) {
            if (verbosity >= Log.INFO)
                Log.i(tag, msg)
        }

        // 5
        fun w(tag: String, msg: String) {
            if (isWarningsEnabled)
                Log.w(tag, msg)
        }

        // 6
        fun e(tag: String, msg: String) {
            if (isErrorsEnabled)
                Log.e(tag, msg)
        }

        // 7
        fun wtf(tag: String, msg: String) {
            Log.wtf(tag, msg)
        }
    }
}
