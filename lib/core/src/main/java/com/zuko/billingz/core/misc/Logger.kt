/*
 *
 *  * Copyright 2021 rjsuzuki
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  *
 *
 */
package com.zuko.billingz.core.misc

import android.util.Log

@Suppress("unused")
object Logger : Logz {

    enum class Level {
        /**
         * Only warnings and error logs will be shown.
         */
        LOW,

        /**
         * Logs equivalent to [Log.VERBOSE] will be shown.
         * Warning and error logs will be shown.
         */
        DEFAULT,

        /**
         * All log types are enabled. Be mindful as this will show sensitive information.
         */
        DEBUG
    }

    /**
     * Change the verbosity level to control the output of the logs.
     * Defaylt is [Level.DEFAULT]
     */
    override var verbosity: Level = Level.DEFAULT

    private const val UNKNOWN = "Unknown Exception"

    // 2
    override fun v(tag: String, msg: String) {
        if (verbosity == Level.DEFAULT || verbosity == Level.DEBUG) {
            Log.v(tag, msg)
        }
    }

    // 3 - sensitive information
    override fun d(tag: String, msg: String) {
        if (verbosity == Level.DEBUG) {
            Log.d(tag, msg)
        }
    }

    // 4 - sensitive information
    override fun i(tag: String, msg: String) {
        if (verbosity == Level.DEBUG) {
            Log.i(tag, msg)
        }
    }

    // 5
    override fun w(tag: String, msg: String) {
        Log.w(tag, msg)
    }

    // 6
    override fun e(tag: String, msg: String?) {
        Log.e(tag, msg ?: UNKNOWN)
    }

    override fun e(tag: String, t: Throwable?) {
        t?.let {
            Log.e(tag, it.message, it.cause)
        } ?: Log.e(tag, UNKNOWN)
    }

    override fun e(tag: String, msg: String?, t: Throwable?) {
        Log.e(tag, msg ?: UNKNOWN, t)
    }

    // 7
    override fun wtf(tag: String, msg: String?, t: Throwable?) {
        if (t == null) Log.wtf(tag, msg ?: UNKNOWN) else Log.wtf(tag, msg, t)
    }
}
