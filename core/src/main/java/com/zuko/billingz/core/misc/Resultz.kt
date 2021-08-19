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
package com.zuko.billingz.core.misc

class Resultz<T> private constructor(val status: Status, val data: T?, val msg: String?) {

    enum class Status { SUCCESS, ERROR, LOADING }

    companion object {
        @JvmStatic
        fun <T> success(data: T): Resultz<T> {
            return Resultz(Status.SUCCESS, data, null)
        }

        @JvmStatic
        fun <T> error(data: T, msg: String?): Resultz<T> {
            return Resultz(Status.ERROR, data, msg)
        }

        @JvmStatic
        fun <T> loading(data: T, msg: String?): Resultz<T> {
            return Resultz(Status.LOADING, data, msg)
        }
    }
}
