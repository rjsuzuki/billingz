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
package com.zuko.billingz.core.store.security

import java.security.MessageDigest

object Securityz {
    /**
     * This will return an hex representation of the hashed data using sha-256
     */
    fun sha256(data: String, salt: String?): String {
        val stringBuilder = StringBuilder()
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = data.toByteArray()
        salt?.let {
            digest.update(it.toByteArray())
        }
        digest.update(bytes)
        for (b in digest.digest()) {
            stringBuilder.append(String.format("%02X", b))
        }
        return stringBuilder.toString().lowercase()
    }
}
