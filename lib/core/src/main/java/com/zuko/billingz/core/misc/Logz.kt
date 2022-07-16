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

sealed interface Logz {

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
    var verbosity: Logger.Level

    fun v(tag: String, msg: String)
    fun d(tag: String, msg: String)
    fun i(tag: String, msg: String)
    fun w(tag: String, msg: String)
    fun e(tag: String, msg: String?)
    fun e(tag: String, t: Throwable?)
    fun e(tag: String, msg: String?, t: Throwable? = null)
    fun wtf(tag: String, msg: String? = null, t: Throwable? = null)
}
